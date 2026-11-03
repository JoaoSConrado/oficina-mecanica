package br.com.oficina.domain.catalogo;

import br.com.oficina.domain.shared.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço do catálogo da oficina (ex.: troca de óleo, alinhamento).
 */
@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "tempo_estimado_minutos")
    private Integer tempoEstimadoMinutos;

    @Column(nullable = false)
    private boolean ativo;

    protected Servico() {
    }

    public Servico(String nome, String descricao, BigDecimal preco, Integer tempoEstimadoMinutos) {
        this.id = UUID.randomUUID();
        this.ativo = true;
        atualizar(nome, descricao, preco, tempoEstimadoMinutos);
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, Integer tempoEstimadoMinutos) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("Nome do serviço é obrigatório");
        }
        if (preco == null || preco.signum() < 0) {
            throw new DadoInvalidoException("Preço do serviço deve ser maior ou igual a zero");
        }
        if (tempoEstimadoMinutos != null && tempoEstimadoMinutos <= 0) {
            throw new DadoInvalidoException("Tempo estimado deve ser positivo");
        }
        this.nome = nome.trim();
        this.descricao = descricao;
        this.preco = preco;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Integer getTempoEstimadoMinutos() {
        return tempoEstimadoMinutos;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
