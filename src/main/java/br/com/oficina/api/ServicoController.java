package br.com.oficina.api;

import br.com.oficina.application.ServicoService;
import br.com.oficina.application.dto.CatalogoDtos.ServicoRequest;
import br.com.oficina.application.dto.CatalogoDtos.ServicoResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponse criar(@Valid @RequestBody ServicoRequest request) {
        return service.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista os serviços ativos do catálogo")
    public List<ServicoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ServicoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ServicoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa o serviço (exclusão lógica)")
    public void remover(@PathVariable UUID id) {
        service.remover(id);
    }
}
