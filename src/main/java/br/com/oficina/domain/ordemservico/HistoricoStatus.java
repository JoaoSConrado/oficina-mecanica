package br.com.oficina.domain.ordemservico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutável de cada mudança de status da OS, usado no acompanhamento pelo cliente.
 */
@Entity
@Table(name = "historico_status_os")
public class HistoricoStatus {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusOrdemServico statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusOrdemServico statusNovo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(length = 500)
    private String observacao;

    protected HistoricoStatus() {
    }

    HistoricoStatus(OrdemServico ordemServico, StatusOrdemServico statusAnterior,
                    StatusOrdemServico statusNovo, Instant ocorridoEm, String observacao) {
        this.id = UUID.randomUUID();
        this.ordemServico = ordemServico;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.ocorridoEm = ocorridoEm;
        this.observacao = observacao;
    }

    public UUID getId() {
        return id;
    }

    public StatusOrdemServico getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOrdemServico getStatusNovo() {
        return statusNovo;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public String getObservacao() {
        return observacao;
    }
}
