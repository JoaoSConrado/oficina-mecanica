package br.com.oficina.api;

import br.com.oficina.application.OrdemServicoService;
import br.com.oficina.application.dto.OrdemServicoDtos.DecisaoClienteRequest;
import br.com.oficina.application.dto.OrdemServicoDtos.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * API pública usada pelo cliente (aplicativo/site) para acompanhar a OS e responder ao orçamento.
 * O cliente prova a titularidade informando o CPF/CNPJ cadastrado.
 */
@RestController
@RequestMapping("/api/public/ordens-servico")
@Tag(name = "Acompanhamento pelo cliente")
@SecurityRequirements
public class AcompanhamentoController {

    private final OrdemServicoService service;

    public AcompanhamentoController(OrdemServicoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o andamento da OS (status, orçamento e histórico)")
    public OrdemServicoResponse consultar(@PathVariable UUID id, @RequestParam String documento) {
        return service.consultarPeloCliente(id, documento);
    }

    @PostMapping("/{id}/orcamento/aprovacao")
    @Operation(summary = "Cliente aprova o orçamento; a execução é iniciada automaticamente")
    public OrdemServicoResponse aprovar(@PathVariable UUID id, @Valid @RequestBody DecisaoClienteRequest request) {
        return service.aprovarPeloCliente(id, request.documento());
    }

    @PostMapping("/{id}/orcamento/rejeicao")
    @Operation(summary = "Cliente rejeita o orçamento; a OS é finalizada sem execução")
    public OrdemServicoResponse rejeitar(@PathVariable UUID id, @Valid @RequestBody DecisaoClienteRequest request) {
        return service.rejeitarPeloCliente(id, request.documento(), request.motivo());
    }
}
