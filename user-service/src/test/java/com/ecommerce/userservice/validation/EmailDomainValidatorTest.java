package com.ecommerce.userservice.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDomainValidatorTest {

    private final EmailDomainValidator validator = new EmailDomainValidator();

    @Test
    void devuelveTrueSiElEmailEsNulo() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void devuelveFalseSiElEmailNoTieneArroba() {
        assertFalse(validator.isValid("sin-arroba", null));
    }

    @Test
    void aceptaDominiosPermitidos() {
        assertTrue(validator.isValid("isaac@gmail.com", null));
        assertTrue(validator.isValid("isaac@outlook.com", null));
        assertTrue(validator.isValid("isaac@hotmail.com", null));
        assertTrue(validator.isValid("isaac@yahoo.com", null));
        assertTrue(validator.isValid("isaac@icloud.com", null));
        assertTrue(validator.isValid("isaac@ids.com", null));
    }

    @Test
    void aceptaElDominioSinImportarMayusculas() {
        assertTrue(validator.isValid("isaac@GMAIL.COM", null));
    }

    @Test
    void rechazaDominiosNoPermitidos() {
        assertFalse(validator.isValid("isaac@dominio-invalido.com", null));
    }
}
