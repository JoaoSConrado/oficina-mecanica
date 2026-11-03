package br.com.oficina.api;

import br.com.oficina.application.VeiculoService;
import br.com.oficina.application.dto.VeiculoDtos.AtualizarVeiculoRequest;
import br.com.oficina.application.dto.VeiculoDtos.CriarVeiculoRequest;
import br.com.oficina.application.dto.VeiculoDtos.VeiculoResponse;
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
@RequestMapping("/api/veiculos")
@Tag(name = "Veículos")
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra veículo (placa no padrão antigo ou Mercosul)")
    public VeiculoResponse criar(@Valid @RequestBody CriarVeiculoRequest request) {
        return service.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista veículos, opcionalmente filtrando por cliente")
    public List<VeiculoResponse> listar(@RequestParam(required = false) UUID clienteId) {
        return service.listar(clienteId);
    }

    @GetMapping("/{id}")
    public VeiculoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public VeiculoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarVeiculoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove veículo sem ordens de serviço")
    public void remover(@PathVariable UUID id) {
        service.remover(id);
    }
}
