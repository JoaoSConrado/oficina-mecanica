package br.com.oficina.application.dto;

import br.com.oficina.application.validacao.PlacaValida;
import br.com.oficina.domain.veiculo.Veiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class VeiculoDtos {

    private VeiculoDtos() {
    }

    public record CriarVeiculoRequest(
            @NotBlank @PlacaValida @Schema(example = "BRA2E19") String placa,
            @NotBlank @Size(max = 60) @Schema(example = "Volkswagen") String marca,
            @NotBlank @Size(max = 60) @Schema(example = "Gol") String modelo,
            @NotNull @Schema(example = "2020") Integer ano,
            @NotNull UUID clienteId) {
    }

    public record AtualizarVeiculoRequest(
            @NotBlank @Size(max = 60) String marca,
            @NotBlank @Size(max = 60) String modelo,
            @NotNull Integer ano,
            @NotNull UUID clienteId) {
    }

    public record VeiculoResponse(UUID id, String placa, String marca, String modelo, int ano,
                                  UUID clienteId, String clienteNome) {
        public static VeiculoResponse de(Veiculo veiculo) {
            return new VeiculoResponse(veiculo.getId(), veiculo.getPlaca().getValor(), veiculo.getMarca(),
                    veiculo.getModelo(), veiculo.getAno(), veiculo.getCliente().getId(), veiculo.getCliente().getNome());
        }
    }
}
