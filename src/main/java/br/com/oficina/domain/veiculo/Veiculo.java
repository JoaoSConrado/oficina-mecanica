package br.com.oficina.domain.veiculo;

import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.Placa;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;

@Entity
@Table(name = "veiculos")
public class Veiculo {

    static final int ANO_MINIMO = 1900;

    @Id
    private UUID id;

    @Embedded
    private Placa placa;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(nullable = false)
    private int ano;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Veiculo() {
    }

    public Veiculo(Placa placa, String marca, String modelo, int ano, Cliente cliente) {
        if (placa == null) {
            throw new DadoInvalidoException("Placa do veículo é obrigatória");
        }
        this.id = UUID.randomUUID();
        this.placa = placa;
        this.criadoEm = Instant.now();
        atualizar(marca, modelo, ano, cliente);
    }

    public void atualizar(String marca, String modelo, int ano, Cliente cliente) {
        if (marca == null || marca.isBlank() || modelo == null || modelo.isBlank()) {
            throw new DadoInvalidoException("Marca e modelo do veículo são obrigatórios");
        }
        int anoMaximo = Year.now().getValue() + 1;
        if (ano < ANO_MINIMO || ano > anoMaximo) {
            throw new DadoInvalidoException("Ano do veículo deve estar entre " + ANO_MINIMO + " e " + anoMaximo);
        }
        if (cliente == null) {
            throw new DadoInvalidoException("Veículo deve pertencer a um cliente");
        }
        this.marca = marca.trim();
        this.modelo = modelo.trim();
        this.ano = ano;
        this.cliente = cliente;
    }

    public boolean pertenceA(Cliente outro) {
        return outro != null && cliente.getId().equals(outro.getId());
    }

    public UUID getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public int getAno() {
        return ano;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
