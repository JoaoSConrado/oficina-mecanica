package br.com.oficina.application.validacao;

import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.Placa;
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
 * Valida a placa (padrão antigo ou Mercosul) usando as regras do objeto de valor {@link Placa}.
 */
@Documented
@Constraint(validatedBy = PlacaValida.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlacaValida {

    String message() default "Placa inválida (use ABC1234 ou ABC1D23)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<PlacaValida, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext context) {
            if (valor == null) {
                return true;
            }
            try {
                Placa.of(valor);
                return true;
            } catch (DadoInvalidoException e) {
                return false;
            }
        }
    }
}
