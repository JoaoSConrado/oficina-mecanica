package br.com.oficina;

import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.catalogo.TipoPeca;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.shared.Documento;
import br.com.oficina.domain.shared.Placa;
import br.com.oficina.domain.veiculo.Veiculo;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fábrica de dados válidos para os testes.
 */
public final class DadosTeste {

    public static final String CPF_VALIDO = "52998224725";
    public static final String CNPJ_VALIDO = "11222333000181";

    private DadosTeste() {
    }

    public static String cpfAleatorio() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = random.nextInt(10);
        }
        d[9] = digitoCpf(d, 9);
        d[10] = digitoCpf(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int n : d) {
            sb.append(n);
        }
        return Documento.isValido(sb.toString()) ? sb.toString() : cpfAleatorio();
    }

    private static int digitoCpf(int[] d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += d[i] * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String placaAleatoria() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        sb.append(random.nextInt(10));
        sb.append((char) ('A' + random.nextInt(26)));
        sb.append(String.format("%02d", random.nextInt(100)));
        return sb.toString();
    }

    public static Cliente cliente() {
        return new Cliente("Maria da Silva", Documento.of(cpfAleatorio()), "maria@email.com", "11999998888");
    }

    public static Veiculo veiculo(Cliente cliente) {
        return new Veiculo(Placa.of(placaAleatoria()), "Volkswagen", "Gol", 2020, cliente);
    }

    public static Servico servico(String nome, String preco) {
        return new Servico(nome, null, new BigDecimal(preco), 30);
    }

    public static Peca peca(String codigo, String preco, int estoque) {
        return new Peca(codigo, "Peça " + codigo, null, TipoPeca.PECA, new BigDecimal(preco), estoque, 1);
    }
}
