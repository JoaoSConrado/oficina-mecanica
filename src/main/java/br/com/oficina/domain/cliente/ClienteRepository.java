package br.com.oficina.domain.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByDocumentoNumero(String numero);

    boolean existsByDocumentoNumero(String numero);
}
