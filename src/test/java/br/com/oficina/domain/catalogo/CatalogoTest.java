package br.com.oficina.domain.catalogo;

import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoTest {

    private Peca filtro() {
        return new Peca(" filtro-01 ", "Filtro de óleo", "desc", TipoPeca.PECA, new BigDecimal("35.90"), 10, 3);
    }

    @Test
    void pecaNormalizaCodigoEControlaEstoque() {
        Peca peca = filtro();

        assertThat(peca.getCodigo()).isEqualTo("FILTRO-01");
        assertThat(peca.getNome()).isEqualTo("Filtro de óleo");
        assertThat(peca.getDescricao()).isEqualTo("desc");
        assertThat(peca.getTipo()).isEqualTo(TipoPeca.PECA);
        assertThat(peca.getPreco()).isEqualByComparingTo("35.90");
        assertThat(peca.getEstoqueMinimo()).isEqualTo(3);
        assertThat(peca.isAtivo()).isTrue();
        assertThat(peca.getId()).isNotNull();

        peca.darBaixa(7);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(3);
        assertThat(peca.isAbaixoDoMinimo()).isTrue();

        peca.repor(5);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(8);
        assertThat(peca.isAbaixoDoMinimo()).isFalse();
    }

    @Test
    void pecaNaoPermiteBaixaMaiorQueSaldo() {
        Peca peca = filtro();

        assertThatThrownBy(() -> peca.darBaixa(11))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(10);
    }

    @Test
    void pecaValidaQuantidadesEDados() {
        Peca peca = filtro();

        assertThatThrownBy(() -> peca.repor(0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> peca.darBaixa(-1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Peca(" ", "x", null, TipoPeca.PECA, BigDecimal.ONE, 1, 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Peca("X", "x", null, TipoPeca.PECA, BigDecimal.ONE, -1, 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> peca.atualizar("", null, TipoPeca.PECA, BigDecimal.ONE, 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> peca.atualizar("x", null, null, BigDecimal.ONE, 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> peca.atualizar("x", null, TipoPeca.INSUMO, new BigDecimal("-1"), 0))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> peca.atualizar("x", null, TipoPeca.INSUMO, BigDecimal.ONE, -1))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void pecaPodeSerDesativada() {
        Peca peca = filtro();
        peca.desativar();
        assertThat(peca.isAtivo()).isFalse();
    }

    @Test
    void servicoValidaEAtualiza() {
        Servico servico = new Servico(" Troca de óleo ", "desc", new BigDecimal("80"), 30);

        assertThat(servico.getNome()).isEqualTo("Troca de óleo");
        assertThat(servico.getDescricao()).isEqualTo("desc");
        assertThat(servico.getTempoEstimadoMinutos()).isEqualTo(30);
        assertThat(servico.isAtivo()).isTrue();

        servico.atualizar("Alinhamento", null, new BigDecimal("120"), null);
        assertThat(servico.getPreco()).isEqualByComparingTo("120");

        servico.desativar();
        assertThat(servico.isAtivo()).isFalse();

        assertThatThrownBy(() -> servico.atualizar(null, null, BigDecimal.ONE, null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> servico.atualizar("x", null, null, null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> servico.atualizar("x", null, BigDecimal.ONE, 0))
                .isInstanceOf(DadoInvalidoException.class);
    }
}
