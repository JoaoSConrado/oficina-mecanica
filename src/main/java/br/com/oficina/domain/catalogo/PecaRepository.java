package br.com.oficina.domain.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    List<Peca> findByAtivoTrueOrderByNome();

    boolean existsByCodigo(String codigo);
}
