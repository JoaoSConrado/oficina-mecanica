package br.com.oficina.application;

import br.com.oficina.domain.ordemservico.OrdemServico;

/**
 * Porta de saída para avisar o cliente sobre eventos da OS (e-mail, SMS, push...).
 */
public interface NotificadorCliente {

    void orcamentoDisponivel(OrdemServico ordemServico);

    void statusAlterado(OrdemServico ordemServico);
}
