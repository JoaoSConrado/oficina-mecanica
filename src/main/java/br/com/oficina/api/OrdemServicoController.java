package br.com.oficina.api;

import br.com.oficina.application.MetricasService;
import br.com.oficina.application.OrdemServicoService;
import br.com.oficina.application.dto.OrdemServicoDtos.AbrirOrdemServicoRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.ItemRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.MetricasExecucaoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.OrdemServicoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.OrdemServicoResumoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.RejeicaoRequest;
import br.com.oficina.domain.ordemservico.StatusOrdemServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens-servico")
@Tag(name = "Ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final MetricasService metricas;

    public OrdemServicoController(OrdemServicoService service, MetricasService metricas) {
        this.service = service;
        this.metricas = metricas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre OS identificando o cliente por CPF/CNPJ; cadastra o veículo se a placa for nova")
    public OrdemServicoResponse abrir(@Valid @RequestBody AbrirOrdemServicoRequest request) {
        return service.abrir(request);
    }

    @GetMapping
    @Operation(summary = "Lista OS na ordem da fila de atendimento (em execução > aguardando aprovação > em diagnóstico > recebida)")
    public List<OrdemServicoResumoResponse> listar(@RequestParam(required = false) StatusOrdemServico status,
                                                   @RequestParam(defaultValue = "false") boolean incluirEncerradas) {
        return service.listar(status, incluirEncerradas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha a OS com itens, orçamento e histórico de status")
    public OrdemServicoResponse detalhar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    @GetMapping("/metricas/tempo-medio")
    @Operation(summary = "Tempo médio de execução das OS concluídas, geral e por serviço")
    public MetricasExecucaoResponse tempoMedio() {
        return metricas.tempoMedioExecucao();
    }

    @PostMapping("/{id}/diagnostico")
    @Operation(summary = "Inicia o diagnóstico (Recebida -> Em diagnóstico)")
    public OrdemServicoResponse iniciarDiagnostico(@PathVariable UUID id) {
        return service.iniciarDiagnostico(id);
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Inclui serviço na OS")
    public OrdemServicoResponse adicionarServico(@PathVariable UUID id, @Valid @RequestBody ItemRequest item) {
        return service.adicionarServico(id, item);
    }

    @DeleteMapping("/{id}/servicos/{itemId}")
    public OrdemServicoResponse removerServico(@PathVariable UUID id, @PathVariable UUID itemId) {
        return service.removerServico(id, itemId);
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Inclui peça ou insumo na OS (verifica disponibilidade em estoque)")
    public OrdemServicoResponse adicionarPeca(@PathVariable UUID id, @Valid @RequestBody ItemRequest item) {
        return service.adicionarPeca(id, item);
    }

    @DeleteMapping("/{id}/pecas/{itemId}")
    public OrdemServicoResponse removerPeca(@PathVariable UUID id, @PathVariable UUID itemId) {
        return service.removerPeca(id, itemId);
    }

    @PostMapping("/{id}/orcamento/envio")
    @Operation(summary = "Gera o orçamento e envia ao cliente (-> Aguardando aprovação)")
    public OrdemServicoResponse enviarOrcamento(@PathVariable UUID id) {
        return service.enviarOrcamento(id);
    }

    @PostMapping("/{id}/orcamento/aprovacao")
    @Operation(summary = "Registra aprovação recebida por outro canal; baixa o estoque e inicia a execução")
    public OrdemServicoResponse aprovarOrcamento(@PathVariable UUID id) {
        return service.aprovarOrcamento(id);
    }

    @PostMapping("/{id}/orcamento/rejeicao")
    @Operation(summary = "Registra rejeição do orçamento; a OS é finalizada sem execução")
    public OrdemServicoResponse rejeitarOrcamento(@PathVariable UUID id,
                                                  @Valid @RequestBody(required = false) RejeicaoRequest request) {
        return service.rejeitarOrcamento(id, request == null ? null : request.motivo());
    }

    @PostMapping("/{id}/finalizacao")
    @Operation(summary = "Conclui a execução (Em execução -> Finalizada)")
    public OrdemServicoResponse finalizar(@PathVariable UUID id) {
        return service.finalizar(id);
    }

    @PostMapping("/{id}/entrega")
    @Operation(summary = "Registra a entrega do veículo (Finalizada -> Entregue)")
    public OrdemServicoResponse entregar(@PathVariable UUID id) {
        return service.entregar(id);
    }
}
