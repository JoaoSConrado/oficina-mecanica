package br.com.oficina.domain.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByAtivoTrueOrderByNome();
}
