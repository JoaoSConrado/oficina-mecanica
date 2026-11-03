package br.com.oficina.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/**
 * Objeto de valor que representa o documento de identificação do cliente (CPF ou CNPJ).
 * <p>
 * Aceita valores com ou sem máscara e valida os dígitos verificadores. O CNPJ segue o
 * cálculo da Receita Federal que também contempla o formato alfanumérico (IN RFB 2.229/2024).
 */
@Embeddable
public class Documento {

    @Column(name = "documento", nullable = false, unique = true, length = 14)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipo;

    protected Documento() {
    }

    private Documento(String numero, TipoDocumento tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public static Documento of(String valor) {
        String normalizado = normalizar(valor);
        if (normalizado.matches("\\d{11}") && cpfValido(normalizado)) {
            return new Documento(normalizado, TipoDocumento.CPF);
        }
        if (normalizado.matches("[A-Z0-9]{12}\\d{2}") && cnpjValido(normalizado)) {
            return new Documento(normalizado, TipoDocumento.CNPJ);
        }
        throw new DadoInvalidoException("CPF/CNPJ inválido: " + valor);
    }

    public static boolean isValido(String valor) {
        try {
            of(valor);
            return true;
        } catch (DadoInvalidoException e) {
            return false;
        }
    }

    public static String normalizar(String valor) {
        return valor == null ? "" : valor.toUpperCase().replaceAll("[\\s./-]", "");
    }

    private static boolean cpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCpf(cpf, 9) == cpf.charAt(9) - '0'
                && digitoCpf(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static boolean cnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCnpj(cnpj, 12) == cnpj.charAt(12) - '0'
                && digitoCnpj(cnpj, 13) == cnpj.charAt(13) - '0';
    }

    private static int digitoCnpj(String cnpj, int tamanho) {
        int soma = 0;
        int peso = 2;
        for (int i = tamanho - 1; i >= 0; i--) {
            // Para o CNPJ alfanumérico, cada caractere vale (código ASCII - 48)
            soma += (cnpj.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public String getNumero() {
        return numero;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String formatado() {
        if (tipo == TipoDocumento.CPF) {
            return numero.replaceFirst("(\\w{3})(\\w{3})(\\w{3})(\\w{2})", "$1.$2.$3-$4");
        }
        return numero.replaceFirst("(\\w{2})(\\w{3})(\\w{3})(\\w{4})(\\w{2})", "$1.$2.$3/$4-$5");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Documento outro && Objects.equals(numero, outro.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(numero);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
