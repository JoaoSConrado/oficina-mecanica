package br.com.oficina.domain.shared;

/**
 * Exceção base para violações de regras do domínio.
 */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
