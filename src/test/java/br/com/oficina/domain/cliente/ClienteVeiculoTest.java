package br.com.oficina.domain.cliente;

import br.com.oficina.DadosTeste;
import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.Documento;
import br.com.oficina.domain.shared.Placa;
import br.com.oficina.domain.veiculo.Veiculo;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteVeiculoTest {

    @Test
    void clienteExigeNomeEDocumento() {
        Cliente cliente = new Cliente(" Maria ", Documento.of(DadosTeste.CPF_VALIDO), "m@x.com", "119");

        assertThat(cliente.getNome()).isEqualTo("Maria");
        assertThat(cliente.getEmail()).isEqualTo("m@x.com");
        assertThat(cliente.getTelefone()).isEqualTo("119");
        assertThat(cliente.getCriadoEm()).isNotNull();

        cliente.atualizar("Maria Souza", null, null);
        assertThat(cliente.getNome()).isEqualTo("Maria Souza");

        assertThatThrownBy(() -> cliente.atualizar(" ", null, null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Cliente("x", null, null, null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void veiculoPertenceAoCliente() {
        Cliente dono = DadosTeste.cliente();
        Cliente outro = DadosTeste.cliente();
        Veiculo veiculo = new Veiculo(Placa.of("ABC1234"), " VW ", " Gol ", 2020, dono);

        assertThat(veiculo.getMarca()).isEqualTo("VW");
        assertThat(veiculo.getModelo()).isEqualTo("Gol");
        assertThat(veiculo.getAno()).isEqualTo(2020);
        assertThat(veiculo.getCriadoEm()).isNotNull();
        assertThat(veiculo.pertenceA(dono)).isTrue();
        assertThat(veiculo.pertenceA(outro)).isFalse();
        assertThat(veiculo.pertenceA(null)).isFalse();

        veiculo.atualizar("Fiat", "Uno", 2010, outro);
        assertThat(veiculo.getCliente()).isSameAs(outro);
    }

    @Test
    void veiculoValidaDados() {
        Cliente dono = DadosTeste.cliente();
        Placa placa = Placa.of("ABC1234");
        int anoFuturo = Year.now().getValue() + 2;

        assertThatThrownBy(() -> new Veiculo(null, "VW", "Gol", 2020, dono)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Veiculo(placa, "", "Gol", 2020, dono)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Veiculo(placa, "VW", null, 2020, dono)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Veiculo(placa, "VW", "Gol", 1899, dono)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Veiculo(placa, "VW", "Gol", anoFuturo, dono)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Veiculo(placa, "VW", "Gol", 2020, null)).isInstanceOf(DadoInvalidoException.class);
    }
}
