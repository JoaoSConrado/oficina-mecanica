package br.com.oficina.application;

import br.com.oficina.DadosTeste;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.ordemservico.OrdemServico;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricasServiceTest {

    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");

    private final OrdemServicoRepository repository = mock(OrdemServicoRepository.class);
    private final MetricasService service = new MetricasService(repository);

    private OrdemServico concluida(List<Servico> servicos, Duration espera, Duration execucao) {
        Cliente cliente = DadosTeste.cliente();
        OrdemServico os = OrdemServico.abrir(cliente, DadosTeste.veiculo(cliente), null, T0);
        servicos.forEach(s -> os.adicionarServico(s, 1, T0));
        os.enviarOrcamento(T0);
        os.aprovarOrcamento(T0.plus(espera));
        os.finalizar(T0.plus(espera).plus(execucao));
        return os;
    }

    @Test
    void calculaMediasGeraisEPorServico() {
        Servico oleo = DadosTeste.servico("Troca de óleo", "80");
        Servico freio = DadosTeste.servico("Freios", "150");
        when(repository.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull()).thenReturn(List.of(
                concluida(List.of(oleo), Duration.ofMinutes(30), Duration.ofMinutes(40)),
                concluida(List.of(oleo, freio), Duration.ofMinutes(10), Duration.ofMinutes(100))));

        var metricas = service.tempoMedioExecucao();

        assertThat(metricas.ordensConsideradas()).isEqualTo(2);
        assertThat(metricas.tempoMedioExecucaoMinutos()).isEqualTo(70.0);
        assertThat(metricas.tempoMedioTotalMinutos()).isEqualTo(90.0);
        assertThat(metricas.porServico()).hasSize(2);
        assertThat(metricas.porServico().get(0).servico()).isEqualTo("Freios");
        assertThat(metricas.porServico().get(0).tempoMedioExecucaoMinutos()).isEqualTo(100.0);
        assertThat(metricas.porServico().get(1).servico()).isEqualTo("Troca de óleo");
        assertThat(metricas.porServico().get(1).ordensConsideradas()).isEqualTo(2);
        assertThat(metricas.porServico().get(1).tempoMedioExecucaoMinutos()).isEqualTo(70.0);
    }

    @Test
    void semOrdensConcluidasRetornaZero() {
        when(repository.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull()).thenReturn(List.of());

        var metricas = service.tempoMedioExecucao();

        assertThat(metricas.ordensConsideradas()).isZero();
        assertThat(metricas.tempoMedioExecucaoMinutos()).isZero();
        assertThat(metricas.porServico()).isEmpty();
    }
}
