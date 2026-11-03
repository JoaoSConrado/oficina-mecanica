package br.com.oficina.application;

import br.com.oficina.application.dto.OrdemServicoDtos.MetricasExecucaoResponse;
import br.com.oficina.application.dto.OrdemServicoDtos.TempoMedioServicoResponse;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.ordemservico.ItemServicoOS;
import br.com.oficina.domain.ordemservico.OrdemServico;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.UUID;

/**
 * Monitoramento do tempo médio de execução dos serviços, considerando as OS cuja execução
 * foi concluída (do início da execução até a finalização).
 */
@Service
public class MetricasService {

    private final OrdemServicoRepository ordens;

    public MetricasService(OrdemServicoRepository ordens) {
        this.ordens = ordens;
    }

    @Transactional(readOnly = true)
    public MetricasExecucaoResponse tempoMedioExecucao() {
        List<OrdemServico> concluidas = ordens.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();

        double mediaExecucao = concluidas.stream()
                .mapToLong(MetricasService::minutosDeExecucao)
                .average().orElse(0);
        double mediaTotal = concluidas.stream()
                .mapToLong(os -> Duration.between(os.getCriadaEm(), os.getFinalizadaEm()).toMinutes())
                .average().orElse(0);

        return new MetricasExecucaoResponse(concluidas.size(), arredondar(mediaExecucao), arredondar(mediaTotal),
                porServico(concluidas));
    }

    private List<TempoMedioServicoResponse> porServico(List<OrdemServico> concluidas) {
        Map<UUID, Servico> servicos = new LinkedHashMap<>();
        Map<UUID, LongSummaryStatistics> estatisticas = new LinkedHashMap<>();
        for (OrdemServico os : concluidas) {
            long minutos = minutosDeExecucao(os);
            for (ItemServicoOS item : os.getServicos()) {
                UUID servicoId = item.getServico().getId();
                servicos.putIfAbsent(servicoId, item.getServico());
                estatisticas.computeIfAbsent(servicoId, id -> new LongSummaryStatistics()).accept(minutos);
            }
        }
        return estatisticas.entrySet().stream()
                .map(e -> new TempoMedioServicoResponse(e.getKey(), servicos.get(e.getKey()).getNome(),
                        e.getValue().getCount(), arredondar(e.getValue().getAverage())))
                .sorted(Comparator.comparing(TempoMedioServicoResponse::servico))
                .toList();
    }

    private static long minutosDeExecucao(OrdemServico os) {
        return os.getTempoExecucao().orElse(Duration.ZERO).toMinutes();
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
