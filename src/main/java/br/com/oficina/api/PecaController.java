package br.com.oficina.api;

import br.com.oficina.application.PecaService;
import br.com.oficina.application.dto.CatalogoDtos.AtualizarPecaRequest;
import br.com.oficina.application.dto.CatalogoDtos.CriarPecaRequest;
import br.com.oficina.application.dto.CatalogoDtos.MovimentoEstoqueRequest;
import br.com.oficina.application.dto.CatalogoDtos.PecaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pecas")
@Tag(name = "Peças e insumos")
public class PecaController {

    private final PecaService service;

    public PecaController(PecaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PecaResponse criar(@Valid @RequestBody CriarPecaRequest request) {
        return service.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista peças e insumos ativos; use abaixoDoMinimo=true para ver o que precisa de reposição")
    public List<PecaResponse> listar(@RequestParam(defaultValue = "false") boolean abaixoDoMinimo) {
        return service.listar(abaixoDoMinimo);
    }

    @GetMapping("/{id}")
    public PecaResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados cadastrais (o saldo muda apenas por entrada/saída de estoque)")
    public PecaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarPecaRequest request) {
        return service.atualizar(id, request);
    }

    @PostMapping("/{id}/estoque/entrada")
    @Operation(summary = "Registra entrada (reposição) no estoque")
    public PecaResponse entrada(@PathVariable UUID id, @Valid @RequestBody MovimentoEstoqueRequest request) {
        return service.registrarEntrada(id, request.quantidade());
    }

    @PostMapping("/{id}/estoque/saida")
    @Operation(summary = "Registra saída manual (perda, uso interno)")
    public PecaResponse saida(@PathVariable UUID id, @Valid @RequestBody MovimentoEstoqueRequest request) {
        return service.registrarSaida(id, request.quantidade());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa a peça (exclusão lógica)")
    public void remover(@PathVariable UUID id) {
        service.remover(id);
    }
}
