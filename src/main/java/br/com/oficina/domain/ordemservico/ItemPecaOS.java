package br.com.oficina.domain.ordemservico;

import br.com.oficina.domain.catalogo.Peca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Peça ou insumo incluído na OS, com preço unitário congelado no momento da inclusão.
 */
@Entity
@Table(name = "itens_peca_os")
public class ItemPecaOS {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPecaOS() {
    }

    ItemPecaOS(OrdemServico ordemServico, Peca peca, int quantidade) {
        this.id = UUID.randomUUID();
        this.ordemServico = ordemServico;
        this.peca = peca;
        this.quantidade = quantidade;
        this.precoUnitario = peca.getPreco();
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

    public Peca getPeca() {
        return peca;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
