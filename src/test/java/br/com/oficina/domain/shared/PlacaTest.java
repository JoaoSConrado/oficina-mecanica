package br.com.oficina.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacaTest {

    @Test
    void aceitaPadraoAntigo() {
        Placa placa = Placa.of("abc-1234");

        assertThat(placa.getValor()).isEqualTo("ABC1234");
        assertThat(placa.isMercosul()).isFalse();
    }

    @Test
    void aceitaPadraoMercosul() {
        Placa placa = Placa.of("BRA2E19");

        assertThat(placa.getValor()).isEqualTo("BRA2E19");
        assertThat(placa.isMercosul()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"AB1234", "ABCD123", "1BC1234", "ABC12345", "ABC1D2E"})
    void rejeitaPlacaInvalida(String valor) {
        assertThatThrownBy(() -> Placa.of(valor)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void igualdadePorValor() {
        assertThat(Placa.of("abc1234")).isEqualTo(Placa.of("ABC-1234"))
                .hasSameHashCodeAs(Placa.of("ABC1234"))
                .hasToString("ABC1234");
    }
}
