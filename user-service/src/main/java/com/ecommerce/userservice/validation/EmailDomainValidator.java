package com.ecommerce.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.Locale;

public class EmailDomainValidator
        implements ConstraintValidator<ValidEmail, String> {

    private static final Set<String> DOMINIOS_PERMITIDOS = Set.of(
            "gmail.com",
            "outlook.com",
            "hotmail.com",
            "yahoo.com",
            "icloud.com",
            "ids.com"
    );

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z]{2,})+$";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {

        if (email == null || email.isBlank()) {
            return false;
        }

        if (!email.matches(EMAIL_REGEX)) {
            return false;
        }

        String dominio = email.substring(email.lastIndexOf('@') + 1)
                .toLowerCase(Locale.ROOT);

        return DOMINIOS_PERMITIDOS.contains(dominio);
    }
}