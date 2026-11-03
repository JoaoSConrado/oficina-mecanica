package br.com.oficina.domain.shared;

/**
 * Dado de entrada inválido (ex.: CPF/CNPJ ou placa fora do padrão).
 */
public class DadoInvalidoException extends DominioException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
