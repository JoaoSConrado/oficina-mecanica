package br.com.oficina.infrastructure.notificacao;

import br.com.oficina.application.NotificadorCliente;
import br.com.oficina.domain.ordemservico.OrdemServico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação do MVP: registra a notificação em log. Pode ser trocada por e-mail, SMS ou
 * push sem alterar os casos de uso.
 */
@Component
public class LogNotificadorCliente implements NotificadorCliente {

    private static final Logger log = LoggerFactory.getLogger(LogNotificadorCliente.class);

    @Override
    public void orcamentoDisponivel(OrdemServico os) {
        log.info("[notificação] Orçamento da OS {} disponível para {} - valor R$ {}",
                os.getId(), os.getCliente().getNome(), os.getValorOrcamento());
    }

    @Override
    public void statusAlterado(OrdemServico os) {
        log.info("[notificação] OS {} de {} agora está '{}'",
                os.getId(), os.getCliente().getNome(), os.getStatus().getDescricao());
    }
}
