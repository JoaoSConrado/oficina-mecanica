package br.com.oficina.application;

import br.com.oficina.application.dto.OrdemServicoDtos.AbrirOrdemServicoRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.ItemRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.OrdemServicoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.OrdemServicoResumoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.VeiculoDaOrdemRequest;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.ordemservico.OrdemServico;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import br.com.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.oficina.domain.shared.Documento;
import br.com.oficina.domain.shared.Placa;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import br.com.oficina.domain.veiculo.Veiculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Casos de uso do fluxo de atendimento: abertura, diagnóstico, orçamento, execução e entrega.
 * Cada operação roda numa única transação, de modo que a baixa de estoque e a mudança de
 * status da OS são confirmadas ou desfeitas juntas.
 */
@Service
public class OrdemServicoService {

    private static final Comparator<OrdemServico> FILA_DE_ATENDIMENTO =
            Comparator.comparingInt((OrdemServico os) -> os.getStatus().getPrioridade())
                    .thenComparing(OrdemServico::getCriadaEm);

    private final OrdemServicoRepository ordens;
    private final ClienteService clienteService;
    private final VeiculoService veiculoService;
    private final ServicoService servicoService;
    private final PecaService pecaService;
    private final NotificadorCliente notificador;
    private final Clock clock;

    public OrdemServicoService(OrdemServicoRepository ordens, ClienteService clienteService,
                               VeiculoService veiculoService, ServicoService servicoService, PecaService pecaService,
                               NotificadorCliente notificador, Clock clock) {
        this.ordens = ordens;
        this.clienteService = clienteService;
        this.veiculoService = veiculoService;
        this.servicoService = servicoService;
        this.pecaService = pecaService;
        this.notificador = notificador;
        this.clock = clock;
    }

    @Transactional
    public OrdemServicoResponse abrir(AbrirOrdemServicoRequest request) {
        Cliente cliente = clienteService.obterPorDocumento(request.documentoCliente());
        Veiculo veiculo = localizarOuCadastrarVeiculo(request.veiculo(), cliente);
        Instant agora = agora();

        OrdemServico os = OrdemServico.abrir(cliente, veiculo, request.descricaoProblema(), agora);
        incluirItens(os, request.servicos(), request.pecas(), agora);
        return OrdemServicoResponse.de(ordens.save(os));
    }

    /**
     * Lista as OS na ordem da fila de atendimento: em execução, aguardando aprovação,
     * em diagnóstico e recebidas, das mais antigas para as mais recentes. OS finalizadas
     * e entregues só aparecem quando filtradas explicitamente ou com {@code incluirEncerradas}.
     */
    @Transactional(readOnly = true)
    public List<OrdemServicoResumoResponse> listar(StatusOrdemServico status, boolean incluirEncerradas) {
        List<StatusOrdemServico> filtro;
        if (status != null) {
            filtro = List.of(status);
        } else {
            filtro = Arrays.stream(StatusOrdemServico.values())
                    .filter(s -> incluirEncerradas || s.isEmAndamento())
                    .toList();
        }
        return ordens.findByStatusIn(filtro).stream()
                .sorted(FILA_DE_ATENDIMENTO)
                .map(OrdemServicoResumoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse detalhar(UUID id) {
        return OrdemServicoResponse.de(obter(id));
    }

    @Transactional
    public OrdemServicoResponse iniciarDiagnostico(UUID id) {
        return executar(id, os -> os.iniciarDiagnostico(agora()));
    }

    @Transactional
    public OrdemServicoResponse adicionarServico(UUID id, ItemRequest item) {
        return executar(id, os -> os.adicionarServico(servicoService.obter(item.id()), item.quantidade(), agora()));
    }

    @Transactional
    public OrdemServicoResponse adicionarPeca(UUID id, ItemRequest item) {
        return executar(id, os -> os.adicionarPeca(pecaService.obter(item.id()), item.quantidade(), agora()));
    }

    @Transactional
    public OrdemServicoResponse removerServico(UUID id, UUID itemId) {
        return executar(id, os -> os.removerServico(itemId));
    }

    @Transactional
    public OrdemServicoResponse removerPeca(UUID id, UUID itemId) {
        return executar(id, os -> os.removerPeca(itemId));
    }

    @Transactional
    public OrdemServicoResponse enviarOrcamento(UUID id) {
        OrdemServico os = obter(id);
        os.enviarOrcamento(agora());
        notificador.orcamentoDisponivel(os);
        return OrdemServicoResponse.de(os);
    }

    @Transactional
    public OrdemServicoResponse aprovarOrcamento(UUID id) {
        return executar(id, os -> os.aprovarOrcamento(agora()));
    }

    @Transactional
    public OrdemServicoResponse rejeitarOrcamento(UUID id, String motivo) {
        return executar(id, os -> os.rejeitarOrcamento(motivo, agora()));
    }

    @Transactional
    public OrdemServicoResponse finalizar(UUID id) {
        return executar(id, os -> os.finalizar(agora()));
    }

    @Transactional
    public OrdemServicoResponse entregar(UUID id) {
        return executar(id, os -> os.entregar(agora()));
    }

    // ---------------------------------------------------------------- acesso do cliente

    @Transactional(readOnly = true)
    public OrdemServicoResponse consultarPeloCliente(UUID id, String documento) {
        return OrdemServicoResponse.de(obterDoCliente(id, documento));
    }

    @Transactional
    public OrdemServicoResponse aprovarPeloCliente(UUID id, String documento) {
        OrdemServico os = obterDoCliente(id, documento);
        os.aprovarOrcamento(agora());
        notificador.statusAlterado(os);
        return OrdemServicoResponse.de(os);
    }

    @Transactional
    public OrdemServicoResponse rejeitarPeloCliente(UUID id, String documento, String motivo) {
        OrdemServico os = obterDoCliente(id, documento);
        os.rejeitarOrcamento(motivo, agora());
        notificador.statusAlterado(os);
        return OrdemServicoResponse.de(os);
    }

    // ---------------------------------------------------------------- apoio

    private OrdemServicoResponse executar(UUID id, Consumer<OrdemServico> acao) {
        OrdemServico os = obter(id);
        var statusAntes = os.getStatus();
        acao.accept(os);
        if (os.getStatus() != statusAntes) {
            notificador.statusAlterado(os);
        }
        return OrdemServicoResponse.de(os);
    }

    private Veiculo localizarOuCadastrarVeiculo(VeiculoDaOrdemRequest dados, Cliente cliente) {
        Placa placa = Placa.of(dados.placa());
        return veiculoService.buscarPorPlaca(placa).orElseGet(() -> {
            if (dados.marca() == null || dados.modelo() == null || dados.ano() == null) {
                throw new RegraNegocioException("Veículo " + placa
                        + " não cadastrado: informe marca, modelo e ano para cadastrá-lo na abertura da OS");
            }
            return veiculoService.cadastrar(placa, dados.marca(), dados.modelo(), dados.ano(), cliente);
        });
    }

    private void incluirItens(OrdemServico os, List<ItemRequest> servicos, List<ItemRequest> pecas, Instant agora) {
        if (servicos != null) {
            servicos.forEach(item -> os.adicionarServico(servicoService.obter(item.id()), item.quantidade(), agora));
        }
        if (pecas != null) {
            pecas.forEach(item -> os.adicionarPeca(pecaService.obter(item.id()), item.quantidade(), agora));
        }
    }

    private OrdemServico obter(UUID id) {
        return ordens.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Ordem de serviço", id));
    }

    /**
     * O cliente só enxerga a própria OS: um documento diferente responde como "não encontrada"
     * para não revelar a existência de OS de terceiros.
     */
    private OrdemServico obterDoCliente(UUID id, String documento) {
        String numero = Documento.of(documento).getNumero();
        return ordens.findById(id)
                .filter(os -> os.pertenceAoCliente(numero))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Ordem de serviço", id));
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
