package br.com.oficina.api;

import br.com.oficina.application.ClienteService;
import br.com.oficina.application.dto.ClienteDtos.AtualizarClienteRequest;
import br.com.oficina.application.dto.ClienteDtos.ClienteResponse;
import br.com.oficina.application.dto.ClienteDtos.CriarClienteRequest;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra cliente (CPF ou CNPJ validados)")
    public ClienteResponse criar(@Valid @RequestBody CriarClienteRequest request) {
        return service.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista clientes ou busca pelo CPF/CNPJ")
    public List<ClienteResponse> listar(@RequestParam(required = false) String documento) {
        return documento == null ? service.listar() : List.of(service.buscarPorDocumento(documento));
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e contato do cliente (o documento não é alterável)")
    public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarClienteRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove cliente sem veículos nem ordens de serviço")
    public void remover(@PathVariable UUID id) {
        service.remover(id);
    }
}
