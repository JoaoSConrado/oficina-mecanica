package br.com.oficina.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", " 529.982.247-25 "})
    void aceitaCpfValidoComOuSemMascara(String valor) {
        Documento documento = Documento.of(valor);

        assertThat(documento.getNumero()).isEqualTo("52998224725");
        assertThat(documento.getTipo()).isEqualTo(TipoDocumento.CPF);
        assertThat(documento.formatado()).isEqualTo("529.982.247-25");
    }

    @Test
    void aceitaCnpjNumerico() {
        Documento documento = Documento.of("11.222.333/0001-81");

        assertThat(documento.getTipo()).isEqualTo(TipoDocumento.CNPJ);
        assertThat(documento.getNumero()).isEqualTo("11222333000181");
        assertThat(documento.formatado()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    void aceitaCnpjAlfanumerico() {
        Documento documento = Documento.of("12.abc.345/01de-35");

        assertThat(documento.getTipo()).isEqualTo(TipoDocumento.CNPJ);
        assertThat(documento.getNumero()).isEqualTo("12ABC34501DE35");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "1234", "11.222.333/0001-80",
            "00000000000000", "ABCDEFGHIJK", "12.ABC.345/01DE-36"})
    void rejeitaDocumentoInvalido(String valor) {
        assertThatThrownBy(() -> Documento.of(valor)).isInstanceOf(DadoInvalidoException.class);
        assertThat(Documento.isValido(valor)).isFalse();
    }

    @Test
    void igualdadePorNumero() {
        assertThat(Documento.of("529.982.247-25")).isEqualTo(Documento.of("52998224725"))
                .hasSameHashCodeAs(Documento.of("52998224725"))
                .hasToString("529.982.247-25");
        assertThat(Documento.of("52998224725")).isNotEqualTo(Documento.of("11222333000181"));
    }
}
