package br.com.oficina.domain.ordemservico;

import java.util.Set;

/**
 * Ciclo de vida da Ordem de Serviço e as transições permitidas entre os status.
 * <p>
 * A prioridade define a ordem da fila de atendimento (menor valor = mais urgente).
 */
public enum StatusOrdemServico {
    RECEBIDA("Recebida", 4),
    EM_DIAGNOSTICO("Em diagnóstico", 3),
    AGUARDANDO_APROVACAO("Aguardando aprovação", 2),
    EM_EXECUCAO("Em execução", 1),
    FINALIZADA("Finalizada", 5),
    ENTREGUE("Entregue", 6);

    private final String descricao;
    private final int prioridade;

    StatusOrdemServico(String descricao, int prioridade) {
        this.descricao = descricao;
        this.prioridade = prioridade;
    }

    public Set<StatusOrdemServico> proximosPermitidos() {
        return switch (this) {
            case RECEBIDA -> Set.of(EM_DIAGNOSTICO);
            case EM_DIAGNOSTICO -> Set.of(AGUARDANDO_APROVACAO);
            // Aprovação leva à execução; rejeição encerra a OS sem execução
            case AGUARDANDO_APROVACAO -> Set.of(EM_EXECUCAO, FINALIZADA);
            case EM_EXECUCAO -> Set.of(FINALIZADA);
            case FINALIZADA -> Set.of(ENTREGUE);
            case ENTREGUE -> Set.of();
        };
    }

    public boolean podeTransicionarPara(StatusOrdemServico destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean permiteAlterarItens() {
        return this == RECEBIDA || this == EM_DIAGNOSTICO;
    }

    public boolean isEmAndamento() {
        return this != FINALIZADA && this != ENTREGUE;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPrioridade() {
        return prioridade;
    }
}
