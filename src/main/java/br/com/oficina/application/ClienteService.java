package br.com.oficina.application;

import br.com.oficina.application.dto.ClienteDtos.AtualizarClienteRequest;
import br.com.oficina.application.dto.ClienteDtos.ClienteResponse;
import br.com.oficina.application.dto.ClienteDtos.CriarClienteRequest;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.cliente.ClienteRepository;
import br.com.oficina.domain.ordemservico.OrdemServicoRepository;
import br.com.oficina.domain.shared.ConflitoException;
import br.com.oficina.domain.shared.Documento;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final OrdemServicoRepository ordens;

    public ClienteService(ClienteRepository clientes, VeiculoRepository veiculos, OrdemServicoRepository ordens) {
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.ordens = ordens;
    }

    @Transactional
    public ClienteResponse criar(CriarClienteRequest request) {
        Documento documento = Documento.of(request.documento());
        if (clientes.existsByDocumentoNumero(documento.getNumero())) {
            throw new ConflitoException("Já existe cliente com o documento " + documento.formatado());
        }
        Cliente cliente = new Cliente(request.nome(), documento, request.email(), request.telefone());
        return ClienteResponse.de(clientes.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clientes.findAll().stream().map(ClienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(UUID id) {
        return ClienteResponse.de(obter(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String documento) {
        return ClienteResponse.de(obterPorDocumento(documento));
    }

    @Transactional
    public ClienteResponse atualizar(UUID id, AtualizarClienteRequest request) {
        Cliente cliente = obter(id);
        cliente.atualizar(request.nome(), request.email(), request.telefone());
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public void remover(UUID id) {
        Cliente cliente = obter(id);
        if (ordens.existsByClienteId(id) || veiculos.existsByClienteId(id)) {
            throw new ConflitoException("Cliente possui veículos ou ordens de serviço e não pode ser removido");
        }
        clientes.delete(cliente);
    }

    Cliente obter(UUID id) {
        return clientes.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", id));
    }

    Cliente obterPorDocumento(String documento) {
        Documento doc = Documento.of(documento);
        return clientes.findByDocumentoNumero(doc.getNumero())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", doc.formatado()));
    }
}
