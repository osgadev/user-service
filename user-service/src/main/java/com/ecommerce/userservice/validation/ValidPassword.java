package com.ecommerce.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT,
        ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$"
)
public @interface ValidPassword {

    String message() default
            "La contraseña requiere mínimo 8 caracteres, mayúscula, minúscula, número y símbolo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
