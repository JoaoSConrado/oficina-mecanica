package br.com.oficina.application.dto;

import br.com.oficina.application.validacao.DocumentoValido;
import br.com.oficina.application.validacao.PlacaValida;
import br.com.oficina.domain.ordemservico.DecisaoOrcamento;
import br.com.oficina.domain.ordemservico.HistoricoStatus;
import br.com.oficina.domain.ordemservico.ItemPecaOS;
import br.com.oficina.domain.ordemservico.ItemServicoOS;
import br.com.oficina.domain.ordemservico.OrdemServico;
import br.com.oficina.domain.ordemservico.StatusOrdemServico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrdemServicoDtos {

    private OrdemServicoDtos() {
    }

    // ---------------------------------------------------------------- requests

    /**
     * Abertura da OS. O cliente é identificado pelo CPF/CNPJ. O veículo é localizado pela placa;
     * se ainda não existir, é cadastrado com marca, modelo e ano informados.
     */
    public record AbrirOrdemServicoRequest(
            @NotBlank @DocumentoValido @Schema(example = "529.982.247-25") String documentoCliente,
            @NotNull @Valid VeiculoDaOrdemRequest veiculo,
            @Size(max = 1000) @Schema(example = "Barulho ao frear e troca de óleo vencida") String descricaoProblema,
            List<@Valid ItemRequest> servicos,
            List<@Valid ItemRequest> pecas) {
    }

    public record VeiculoDaOrdemRequest(
            @NotBlank @PlacaValida @Schema(example = "BRA2E19") String placa,
            @Size(max = 60) @Schema(example = "Volkswagen") String marca,
            @Size(max = 60) @Schema(example = "Gol") String modelo,
            @Schema(example = "2020") Integer ano) {
    }

    public record ItemRequest(
            @NotNull @Schema(description = "Id do serviço ou da peça no catálogo") UUID id,
            @NotNull @Positive @Schema(example = "1") Integer quantidade) {
    }

    public record RejeicaoRequest(@Size(max = 500) @Schema(example = "Valor acima do esperado") String motivo) {
    }

    public record DecisaoClienteRequest(
            @NotBlank @DocumentoValido @Schema(description = "CPF/CNPJ do titular da OS", example = "529.982.247-25") String documento,
            @Size(max = 500) String motivo) {
    }

    // ---------------------------------------------------------------- responses

    public record ItemServicoResponse(UUID itemId, UUID servicoId, String nome, int quantidade,
                                      BigDecimal precoUnitario, BigDecimal subtotal) {
        static ItemServicoResponse de(ItemServicoOS item) {
            return new ItemServicoResponse(item.getId(), item.getServico().getId(), item.getServico().getNome(),
                    item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal());
        }
    }

    public record ItemPecaResponse(UUID itemId, UUID pecaId, String codigo, String nome, int quantidade,
                                   BigDecimal precoUnitario, BigDecimal subtotal) {
        static ItemPecaResponse de(ItemPecaOS item) {
            return new ItemPecaResponse(item.getId(), item.getPeca().getId(), item.getPeca().getCodigo(),
                    item.getPeca().getNome(), item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal());
        }
    }

    public record HistoricoResponse(StatusOrdemServico statusAnterior, StatusOrdemServico statusNovo,
                                    Instant ocorridoEm, String observacao) {
        static HistoricoResponse de(HistoricoStatus h) {
            return new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(), h.getOcorridoEm(), h.getObservacao());
        }
    }

    public record ClienteResumo(UUID id, String nome, String documento) {
    }

    public record VeiculoResumo(UUID id, String placa, String marca, String modelo, int ano) {
    }

    public record OrdemServicoResponse(
            UUID id,
            StatusOrdemServico status,
            String statusDescricao,
            ClienteResumo cliente,
            VeiculoResumo veiculo,
            String descricaoProblema,
            List<ItemServicoResponse> servicos,
            List<ItemPecaResponse> pecas,
            @Schema(description = "Total calculado a partir dos itens atuais") BigDecimal valorTotal,
            @Schema(description = "Valor congelado no envio do orçamento") BigDecimal valorOrcamento,
            DecisaoOrcamento decisaoOrcamento,
            String motivoRejeicao,
            Instant criadaEm,
            Instant orcamentoEnviadoEm,
            Instant orcamentoRespondidoEm,
            Instant execucaoIniciadaEm,
            Instant finalizadaEm,
            Instant entregueEm,
            Long tempoExecucaoMinutos,
            List<HistoricoResponse> historico) {

        public static OrdemServicoResponse de(OrdemServico os) {
            var cliente = os.getCliente();
            var veiculo = os.getVeiculo();
            return new OrdemServicoResponse(
                    os.getId(),
                    os.getStatus(),
                    os.getStatus().getDescricao(),
                    new ClienteResumo(cliente.getId(), cliente.getNome(), cliente.getDocumento().formatado()),
                    new VeiculoResumo(veiculo.getId(), veiculo.getPlaca().getValor(), veiculo.getMarca(),
                            veiculo.getModelo(), veiculo.getAno()),
                    os.getDescricaoProblema(),
                    os.getServicos().stream().map(ItemServicoResponse::de).toList(),
                    os.getPecas().stream().map(ItemPecaResponse::de).toList(),
                    os.calcularTotal(),
                    os.getValorOrcamento(),
                    os.getDecisaoOrcamento(),
                    os.getMotivoRejeicao(),
                    os.getCriadaEm(),
                    os.getOrcamentoEnviadoEm(),
                    os.getOrcamentoRespondidoEm(),
                    os.getExecucaoIniciadaEm(),
                    os.getFinalizadaEm(),
                    os.getEntregueEm(),
                    os.getTempoExecucao().map(Duration::toMinutes).orElse(null),
                    os.getHistorico().stream().map(HistoricoResponse::de).toList());
        }
    }

    public record OrdemServicoResumoResponse(UUID id, StatusOrdemServico status, String statusDescricao,
                                             String clienteNome, String placa, BigDecimal valorTotal,
                                             Instant criadaEm) {
        public static OrdemServicoResumoResponse de(OrdemServico os) {
            return new OrdemServicoResumoResponse(os.getId(), os.getStatus(), os.getStatus().getDescricao(),
                    os.getCliente().getNome(), os.getVeiculo().getPlaca().getValor(), os.calcularTotal(),
                    os.getCriadaEm());
        }
    }

    // ---------------------------------------------------------------- métricas

    public record TempoMedioServicoResponse(UUID servicoId, String servico, long ordensConsideradas,
                                            double tempoMedioExecucaoMinutos) {
    }

    public record MetricasExecucaoResponse(
            @Schema(description = "Quantidade de OS com execução concluída") long ordensConsideradas,
            @Schema(description = "Média entre o início da execução e a finalização") double tempoMedioExecucaoMinutos,
            @Schema(description = "Média entre a abertura da OS e a finalização") double tempoMedioTotalMinutos,
            List<TempoMedioServicoResponse> porServico) {
    }
}
