package br.com.oficina.application.validacao;

import br.com.oficina.domain.shared.Documento;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida CPF ou CNPJ (com ou sem máscara) usando as regras do objeto de valor {@link Documento}.
 */
@Documented
@Constraint(validatedBy = DocumentoValido.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentoValido {

    String message() default "CPF/CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<DocumentoValido, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext context) {
            return valor == null || Documento.isValido(valor);
        }
    }
}
