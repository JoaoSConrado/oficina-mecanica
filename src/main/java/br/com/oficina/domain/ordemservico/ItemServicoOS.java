package br.com.oficina.domain.ordemservico;

import br.com.oficina.domain.catalogo.Servico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço incluído na OS. O preço unitário é congelado no momento da inclusão para que
 * alterações posteriores no catálogo não mudem um orçamento já gerado.
 */
@Entity
@Table(name = "itens_servico_os")
public class ItemServicoOS {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemServicoOS() {
    }

    ItemServicoOS(OrdemServico ordemServico, Servico servico, int quantidade) {
        this.id = UUID.randomUUID();
        this.ordemServico = ordemServico;
        this.servico = servico;
        this.quantidade = quantidade;
        this.precoUnitario = servico.getPreco();
    }

    void adicionarQuantidade(int adicional) {
        this.quantidade += adicional;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
