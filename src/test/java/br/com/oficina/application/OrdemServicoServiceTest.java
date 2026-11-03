package br.com.oficina.application;

import br.com.oficina.DadosTeste;
import br.com.oficina.application.dto.OrdemServicoDtos.AbrirOrdemServicoRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.ItemRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.VeiculoDaOrdemRequest;
import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.ordemservico.OrdemServico;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import br.com.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.oficina.domain.shared.Placa;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import br.com.oficina.domain.veiculo.Veiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdemServicoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-05-01T10:00:00Z");

    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final ClienteService clienteService = mock(ClienteService.class);
    private final VeiculoService veiculoService = mock(VeiculoService.class);
    private final ServicoService servicoService = mock(ServicoService.class);
    private final PecaService pecaService = mock(PecaService.class);
    private final NotificadorCliente notificador = mock(NotificadorCliente.class);

    private OrdemServicoService service;
    private Cliente cliente;
    private Veiculo veiculo;
    private Servico servico;
    private Peca peca;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoService(ordens, clienteService, veiculoService, servicoService, pecaService,
                notificador, Clock.fixed(AGORA, ZoneOffset.UTC));
        cliente = DadosTeste.cliente();
        veiculo = DadosTeste.veiculo(cliente);
        servico = DadosTeste.servico("Alinhamento", "120.00");
        peca = DadosTeste.peca("PASTILHA", "190.00", 4);
        when(clienteService.obterPorDocumento(any())).thenReturn(cliente);
        when(servicoService.obter(servico.getId())).thenReturn(servico);
        when(pecaService.obter(peca.getId())).thenReturn(peca);
        when(ordens.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AbrirOrdemServicoRequest pedido(VeiculoDaOrdemRequest dadosVeiculo) {
        return new AbrirOrdemServicoRequest(cliente.getDocumento().getNumero(), dadosVeiculo, "Barulho",
                List.of(new ItemRequest(servico.getId(), 1)), List.of(new ItemRequest(peca.getId(), 2)));
    }

    @Test
    void abreOsComVeiculoExistenteEItens() {
        when(veiculoService.buscarPorPlaca(veiculo.getPlaca())).thenReturn(Optional.of(veiculo));

        var resposta = service.abrir(pedido(new VeiculoDaOrdemRequest(veiculo.getPlaca().getValor(), null, null, null)));

        assertThat(resposta.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("500.00");
        assertThat(resposta.criadaEm()).isEqualTo(AGORA);
        assertThat(resposta.veiculo().placa()).isEqualTo(veiculo.getPlaca().getValor());
        verify(veiculoService, never()).cadastrar(any(), any(), any(), any(), any());
    }

    @Test
    void cadastraVeiculoNovoNaAbertura() {
        Placa placa = Placa.of("XYZ9A88");
        Veiculo novo = new Veiculo(placa, "Fiat", "Argo", 2023, cliente);
        when(veiculoService.buscarPorPlaca(placa)).thenReturn(Optional.empty());
        when(veiculoService.cadastrar(eq(placa), eq("Fiat"), eq("Argo"), eq(2023), eq(cliente))).thenReturn(novo);

        var resposta = service.abrir(pedido(new VeiculoDaOrdemRequest("XYZ-9A88", "Fiat", "Argo", 2023)));

        assertThat(resposta.veiculo().modelo()).isEqualTo("Argo");
    }

    @Test
    void exigeDadosParaCadastrarVeiculoNovo() {
        when(veiculoService.buscarPorPlaca(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(pedido(new VeiculoDaOrdemRequest("XYZ9A88", "Fiat", null, null))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("informe marca, modelo e ano");
    }

    @Test
    void fluxoDeOrcamentoNotificaCliente() {
        OrdemServico os = OrdemServico.abrir(cliente, veiculo, null, AGORA);
        when(ordens.findById(os.getId())).thenReturn(Optional.of(os));

        service.adicionarServico(os.getId(), new ItemRequest(servico.getId(), 1));
        service.adicionarPeca(os.getId(), new ItemRequest(peca.getId(), 1));
        var enviada = service.enviarOrcamento(os.getId());
        var aprovada = service.aprovarPeloCliente(os.getId(), cliente.getDocumento().formatado());

        assertThat(enviada.valorOrcamento()).isEqualByComparingTo("310.00");
        assertThat(aprovada.status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(3);
        verify(notificador).orcamentoDisponivel(os);

        service.finalizar(os.getId());
        var entregue = service.entregar(os.getId());
        assertThat(entregue.status()).isEqualTo(StatusOrdemServico.ENTREGUE);
    }

    @Test
    void clienteRejeitaOrcamento() {
        OrdemServico os = OrdemServico.abrir(cliente, veiculo, null, AGORA);
        os.adicionarServico(servico, 1, AGORA);
        os.enviarOrcamento(AGORA);
        when(ordens.findById(os.getId())).thenReturn(Optional.of(os));

        var resposta = service.rejeitarPeloCliente(os.getId(), cliente.getDocumento().getNumero(), "Sem dinheiro");

        assertThat(resposta.status()).isEqualTo(StatusOrdemServico.FINALIZADA);
        assertThat(resposta.motivoRejeicao()).isEqualTo("Sem dinheiro");
        verify(notificador).statusAlterado(os);
    }

    @Test
    void clienteNaoAcessaOsDeTerceiros() {
        OrdemServico os = OrdemServico.abrir(cliente, veiculo, null, AGORA);
        when(ordens.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.consultarPeloCliente(os.getId(), DadosTeste.CNPJ_VALIDO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(service.consultarPeloCliente(os.getId(), cliente.getDocumento().getNumero()).id())
                .isEqualTo(os.getId());
    }

    @Test
    void listaNaOrdemDaFila() {
        OrdemServico recebida = OrdemServico.abrir(cliente, veiculo, null, AGORA.minusSeconds(3600));
        OrdemServico emExecucao = OrdemServico.abrir(cliente, veiculo, null, AGORA);
        emExecucao.adicionarServico(servico, 1, AGORA);
        emExecucao.enviarOrcamento(AGORA);
        emExecucao.aprovarOrcamento(AGORA);
        when(ordens.findByStatusIn(anyCollection())).thenReturn(List.of(recebida, emExecucao));

        var lista = service.listar(null, false);

        assertThat(lista).extracting(r -> r.status())
                .containsExactly(StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.RECEBIDA);
    }

    @Test
    void osInexistente() {
        when(ordens.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalhar(java.util.UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
