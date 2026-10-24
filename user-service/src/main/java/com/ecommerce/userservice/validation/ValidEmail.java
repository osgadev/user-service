package com.ecommerce.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = EmailDomainValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    String message() default "El correo no es válido o el dominio no está permitido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}