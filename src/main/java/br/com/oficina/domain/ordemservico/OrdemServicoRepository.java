package br.com.oficina.domain.ordemservico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {

    List<OrdemServico> findByStatusIn(Collection<StatusOrdemServico> status);

    List<OrdemServico> findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();

    boolean existsByClienteId(UUID clienteId);

    boolean existsByVeiculoId(UUID veiculoId);
}
