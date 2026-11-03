package br.com.oficina.application;

import br.com.oficina.application.dto.CatalogoDtos.AtualizarPecaRequest;
import br.com.oficina.application.dto.CatalogoDtos.CriarPecaRequest;
import br.com.oficina.application.dto.CatalogoDtos.PecaResponse;
import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.PecaRepository;
import br.com.oficina.domain.shared.ConflitoException;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PecaService {

    private final PecaRepository pecas;

    public PecaService(PecaRepository pecas) {
        this.pecas = pecas;
    }

    @Transactional
    public PecaResponse criar(CriarPecaRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        if (pecas.existsByCodigo(codigo)) {
            throw new ConflitoException("Já existe peça com o código " + codigo);
        }
        Peca peca = new Peca(codigo, request.nome(), request.descricao(), request.tipo(), request.preco(),
                request.quantidadeEstoque(), request.estoqueMinimo());
        return PecaResponse.de(pecas.save(peca));
    }

    @Transactional(readOnly = true)
    public List<PecaResponse> listar(boolean somenteAbaixoDoMinimo) {
        return pecas.findByAtivoTrueOrderByNome().stream()
                .filter(peca -> !somenteAbaixoDoMinimo || peca.isAbaixoDoMinimo())
                .map(PecaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PecaResponse buscar(UUID id) {
        return PecaResponse.de(obter(id));
    }

    @Transactional
    public PecaResponse atualizar(UUID id, AtualizarPecaRequest request) {
        Peca peca = obter(id);
        peca.atualizar(request.nome(), request.descricao(), request.tipo(), request.preco(), request.estoqueMinimo());
        return PecaResponse.de(peca);
    }

    @Transactional
    public PecaResponse registrarEntrada(UUID id, int quantidade) {
        Peca peca = obter(id);
        peca.repor(quantidade);
        return PecaResponse.de(peca);
    }

    @Transactional
    public PecaResponse registrarSaida(UUID id, int quantidade) {
        Peca peca = obter(id);
        peca.darBaixa(quantidade);
        return PecaResponse.de(peca);
    }

    /**
     * Exclusão lógica: a peça sai do catálogo, mas continua referenciada nas OS antigas.
     */
    @Transactional
    public void remover(UUID id) {
        obter(id).desativar();
    }

    Peca obter(UUID id) {
        return pecas.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Peça", id));
    }
}
