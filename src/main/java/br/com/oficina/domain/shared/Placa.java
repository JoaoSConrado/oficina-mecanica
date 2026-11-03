package br.com.oficina.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Objeto de valor da placa do veículo. Aceita o padrão antigo (ABC1234) e o padrão
 * Mercosul (ABC1D23), com ou sem hífen.
 */
@Embeddable
public class Placa {

    private static final String PADRAO = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}";

    @Column(name = "placa", nullable = false, unique = true, length = 7)
    private String valor;

    protected Placa() {
    }

    private Placa(String valor) {
        this.valor = valor;
    }

    public static Placa of(String valor) {
        String normalizada = normalizar(valor);
        if (!normalizada.matches(PADRAO)) {
            throw new DadoInvalidoException("Placa inválida: " + valor);
        }
        return new Placa(normalizada);
    }

    public static String normalizar(String valor) {
        return valor == null ? "" : valor.toUpperCase().replaceAll("[\\s-]", "");
    }

    public boolean isMercosul() {
        return Character.isLetter(valor.charAt(4));
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Placa outra && Objects.equals(valor, outra.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
