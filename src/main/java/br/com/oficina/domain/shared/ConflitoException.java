package br.com.oficina.domain.shared;

/**
 * Conflito com o estado atual dos dados (ex.: documento ou placa já cadastrados).
 */
public class ConflitoException extends DominioException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
