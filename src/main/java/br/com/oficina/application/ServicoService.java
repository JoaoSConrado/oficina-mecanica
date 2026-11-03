package br.com.oficina.application;

import br.com.oficina.application.dto.CatalogoDtos.ServicoRequest;
import br.com.oficina.application.dto.CatalogoDtos.ServicoResponse;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.catalogo.ServicoRepository;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServicoService {

    private final ServicoRepository servicos;

    public ServicoService(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = new Servico(request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ServicoResponse.de(servicos.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar() {
        return servicos.findByAtivoTrueOrderByNome().stream().map(ServicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(UUID id) {
        return ServicoResponse.de(obter(id));
    }

    @Transactional
    public ServicoResponse atualizar(UUID id, ServicoRequest request) {
        Servico servico = obter(id);
        servico.atualizar(request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ServicoResponse.de(servico);
    }

    /**
     * Exclusão lógica: o serviço sai do catálogo, mas continua referenciado nas OS antigas.
     */
    @Transactional
    public void remover(UUID id) {
        obter(id).desativar();
    }

    Servico obter(UUID id) {
        return servicos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));
    }
}
