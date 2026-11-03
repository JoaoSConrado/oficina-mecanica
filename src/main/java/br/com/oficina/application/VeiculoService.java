package br.com.oficina.application;

import br.com.oficina.application.dto.VeiculoDtos.AtualizarVeiculoRequest;
import br.com.oficina.application.dto.VeiculoDtos.CriarVeiculoRequest;
import br.com.oficina.application.dto.VeiculoDtos.VeiculoResponse;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import br.com.oficina.domain.shared.ConflitoException;
import br.com.oficina.domain.shared.Placa;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.veiculo.Veiculo;
import br.com.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculos;
    private final OrdemServicoRepository ordens;
    private final ClienteService clienteService;

    public VeiculoService(VeiculoRepository veiculos, OrdemServicoRepository ordens, ClienteService clienteService) {
        this.veiculos = veiculos;
        this.ordens = ordens;
        this.clienteService = clienteService;
    }

    @Transactional
    public VeiculoResponse criar(CriarVeiculoRequest request) {
        Cliente cliente = clienteService.obter(request.clienteId());
        return VeiculoResponse.de(cadastrar(Placa.of(request.placa()), request.marca(), request.modelo(),
                request.ano(), cliente));
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponse> listar(UUID clienteId) {
        List<Veiculo> resultado = clienteId == null ? veiculos.findAll() : veiculos.findByClienteId(clienteId);
        return resultado.stream().map(VeiculoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscar(UUID id) {
        return VeiculoResponse.de(obter(id));
    }

    @Transactional
    public VeiculoResponse atualizar(UUID id, AtualizarVeiculoRequest request) {
        Veiculo veiculo = obter(id);
        Cliente cliente = clienteService.obter(request.clienteId());
        veiculo.atualizar(request.marca(), request.modelo(), request.ano(), cliente);
        return VeiculoResponse.de(veiculo);
    }

    @Transactional
    public void remover(UUID id) {
        Veiculo veiculo = obter(id);
        if (ordens.existsByVeiculoId(id)) {
            throw new ConflitoException("Veículo possui ordens de serviço e não pode ser removido");
        }
        veiculos.delete(veiculo);
    }

    Veiculo cadastrar(Placa placa, String marca, String modelo, Integer ano, Cliente cliente) {
        if (veiculos.existsByPlacaValor(placa.getValor())) {
            throw new ConflitoException("Já existe veículo com a placa " + placa);
        }
        return veiculos.save(new Veiculo(placa, marca, modelo, ano == null ? 0 : ano, cliente));
    }

    Optional<Veiculo> buscarPorPlaca(Placa placa) {
        return veiculos.findByPlacaValor(placa.getValor());
    }

    private Veiculo obter(UUID id) {
        return veiculos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Veículo", id));
    }
}
