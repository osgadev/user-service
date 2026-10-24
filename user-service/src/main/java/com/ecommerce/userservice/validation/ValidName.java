package com.ecommerce.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = "[\\p{L}]+(?:[ ]+[\\p{L}]+)*")
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {

    String message() default
            "Solo se permiten letras y espacios";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
