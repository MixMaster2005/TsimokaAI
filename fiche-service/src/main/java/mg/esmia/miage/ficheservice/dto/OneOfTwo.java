package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OneOfTwoValidator.class)
public @interface OneOfTwo {
    String message() default "Il faut fournir exactement un des deux : groupeId ou destinataireId";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class OneOfTwoValidator implements ConstraintValidator<OneOfTwo, ShareFicheRequest> {
    @Override
    public boolean isValid(ShareFicheRequest value, ConstraintValidatorContext context) {
        if (value == null) return true;
        boolean hasGroupe = value.groupeId() != null;
        boolean hasDestinataire = value.destinataireId() != null;
        return hasGroupe ^ hasDestinataire;
    }
}
