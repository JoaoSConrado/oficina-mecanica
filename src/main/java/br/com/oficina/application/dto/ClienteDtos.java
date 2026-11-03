package br.com.oficina.application.dto;

import br.com.oficina.application.validacao.DocumentoValido;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.shared.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record CriarClienteRequest(
            @NotBlank @Size(max = 150) @Schema(example = "Maria da Silva") String nome,
            @NotBlank @DocumentoValido @Schema(example = "529.982.247-25") String documento,
            @Email @Size(max = 150) @Schema(example = "maria@email.com") String email,
            @Size(max = 20) @Schema(example = "11999998888") String telefone) {
    }

    public record AtualizarClienteRequest(
            @NotBlank @Size(max = 150) String nome,
            @Email @Size(max = 150) String email,
            @Size(max = 20) String telefone) {
    }

    public record ClienteResponse(UUID id, String nome, String documento, TipoDocumento tipoDocumento,
                                  String email, String telefone, Instant criadoEm) {
        public static ClienteResponse de(Cliente cliente) {
            return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getDocumento().formatado(),
                    cliente.getDocumento().getTipo(), cliente.getEmail(), cliente.getTelefone(), cliente.getCriadoEm());
        }
    }
}
