package br.com.oficina.domain.veiculo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    Optional<Veiculo> findByPlacaValor(String placa);

    boolean existsByPlacaValor(String placa);

    List<Veiculo> findByClienteId(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);
}
