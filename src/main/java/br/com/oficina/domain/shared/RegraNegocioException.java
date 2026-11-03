package br.com.oficina.domain.shared;

/**
 * Operação que viola uma regra de negócio (ex.: transição de status não permitida, estoque insuficiente).
 */
public class RegraNegocioException extends DominioException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
