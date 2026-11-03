package br.com.oficina.api;

import br.com.oficina.domain.shared.ConflitoException;
import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas RFC 7807 (application/problem+json).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
    }

    @ExceptionHandler(DadoInvalidoException.class)
    ProblemDetail dadoInvalido(DadoInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Dado inválido", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concorrencia(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Alteração concorrente",
                "O registro foi alterado por outra operação. Tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Violação de integridade", "Operação viola uma restrição dos dados");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail autenticacao(AuthenticationException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", "Usuário ou senha inválidos");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail validacaoParametros(HandlerMethodValidationException e) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os parâmetros informados");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
