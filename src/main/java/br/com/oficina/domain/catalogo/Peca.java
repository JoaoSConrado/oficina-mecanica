package br.com.oficina.domain.catalogo;

import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Peça ou insumo com controle de estoque. O versionamento otimista evita que duas
 * ordens de serviço aprovadas ao mesmo tempo baixem o mesmo saldo.
 */
@Entity
@Table(name = "pecas")
public class Peca {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPeca tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(nullable = false)
    private boolean ativo;

    @Version
    private Long version;

    protected Peca() {
    }

    public Peca(String codigo, String nome, String descricao, TipoPeca tipo, BigDecimal preco,
                int quantidadeInicial, int estoqueMinimo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DadoInvalidoException("Código da peça é obrigatório");
        }
        if (quantidadeInicial < 0) {
            throw new DadoInvalidoException("Quantidade inicial não pode ser negativa");
        }
        this.id = UUID.randomUUID();
        this.codigo = codigo.trim().toUpperCase();
        this.quantidadeEstoque = quantidadeInicial;
        this.ativo = true;
        atualizar(nome, descricao, tipo, preco, estoqueMinimo);
    }

    public void atualizar(String nome, String descricao, TipoPeca tipo, BigDecimal preco, int estoqueMinimo) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("Nome da peça é obrigatório");
        }
        if (tipo == null) {
            throw new DadoInvalidoException("Tipo (PECA ou INSUMO) é obrigatório");
        }
        if (preco == null || preco.signum() < 0) {
            throw new DadoInvalidoException("Preço da peça deve ser maior ou igual a zero");
        }
        if (estoqueMinimo < 0) {
            throw new DadoInvalidoException("Estoque mínimo não pode ser negativo");
        }
        this.nome = nome.trim();
        this.descricao = descricao;
        this.tipo = tipo;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean possuiEstoque(int quantidade) {
        return quantidadeEstoque >= quantidade;
    }

    public void darBaixa(int quantidade) {
        validarQuantidade(quantidade);
        if (!possuiEstoque(quantidade)) {
            throw new RegraNegocioException("Estoque insuficiente para " + nome + ": disponível "
                    + quantidadeEstoque + ", solicitado " + quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void repor(int quantidade) {
        validarQuantidade(quantidade);
        this.quantidadeEstoque += quantidade;
    }

    public boolean isAbaixoDoMinimo() {
        return quantidadeEstoque <= estoqueMinimo;
    }

    public void desativar() {
        this.ativo = false;
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("Quantidade deve ser maior que zero");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoPeca getTipo() {
        return tipo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
