package br.com.oficina.application.dto;

import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.catalogo.TipoPeca;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record ServicoRequest(
            @NotBlank @Size(max = 120) @Schema(example = "Troca de óleo") String nome,
            @Size(max = 500) String descricao,
            @NotNull @DecimalMin("0.00") @Schema(example = "80.00") BigDecimal preco,
            @Positive @Schema(example = "30") Integer tempoEstimadoMinutos) {
    }

    public record ServicoResponse(UUID id, String nome, String descricao, BigDecimal preco,
                                  Integer tempoEstimadoMinutos, boolean ativo) {
        public static ServicoResponse de(Servico servico) {
            return new ServicoResponse(servico.getId(), servico.getNome(), servico.getDescricao(), servico.getPreco(),
                    servico.getTempoEstimadoMinutos(), servico.isAtivo());
        }
    }

    public record CriarPecaRequest(
            @NotBlank @Size(max = 40) @Schema(example = "FILTRO-OLEO-01") String codigo,
            @NotBlank @Size(max = 120) @Schema(example = "Filtro de óleo") String nome,
            @Size(max = 500) String descricao,
            @NotNull TipoPeca tipo,
            @NotNull @DecimalMin("0.00") @Schema(example = "35.90") BigDecimal preco,
            @NotNull @PositiveOrZero @Schema(example = "20") Integer quantidadeEstoque,
            @NotNull @PositiveOrZero @Schema(example = "5") Integer estoqueMinimo) {
    }

    public record AtualizarPecaRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @NotNull TipoPeca tipo,
            @NotNull @DecimalMin("0.00") BigDecimal preco,
            @NotNull @PositiveOrZero Integer estoqueMinimo) {
    }

    public record MovimentoEstoqueRequest(@NotNull @Positive @Schema(example = "10") Integer quantidade) {
    }

    public record PecaResponse(UUID id, String codigo, String nome, String descricao, TipoPeca tipo, BigDecimal preco,
                               int quantidadeEstoque, int estoqueMinimo, boolean abaixoDoMinimo, boolean ativo) {
        public static PecaResponse de(Peca peca) {
            return new PecaResponse(peca.getId(), peca.getCodigo(), peca.getNome(), peca.getDescricao(), peca.getTipo(),
                    peca.getPreco(), peca.getQuantidadeEstoque(), peca.getEstoqueMinimo(), peca.isAbaixoDoMinimo(),
                    peca.isAtivo());
        }
    }
}
