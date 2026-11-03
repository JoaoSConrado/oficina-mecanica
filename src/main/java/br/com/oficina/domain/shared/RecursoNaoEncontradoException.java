package br.com.oficina.domain.shared;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object identificador) {
        return new RecursoNaoEncontradoException(recurso + " não encontrado(a): " + identificador);
    }
}
