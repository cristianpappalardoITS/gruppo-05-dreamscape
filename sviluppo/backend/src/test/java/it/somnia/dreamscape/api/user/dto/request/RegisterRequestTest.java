package it.somnia.dreamscape.api.user.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

// Regole dei campi della registrazione (guida per il frontend), senza avviare Spring
class RegisterRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validate_correctFields_hasNoErrors() {
        // Given
        RegisterRequest request = new RegisterRequest("lucida", "lucida@example.com", "una-password-lunga", true, true);

        // When
        Set<ConstraintViolation<RegisterRequest>> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_usernameWithSpace_reportsUsername() {
        // Given
        RegisterRequest request = new RegisterRequest("lu cida", "lucida@example.com", "una-password-lunga", true, true);

        // When
        Set<ConstraintViolation<RegisterRequest>> errors = validator.validate(request);

        // Then
        assertEquals(Set.of("username"), fieldsOf(errors));
    }

    @Test
    void validate_shortPassword_reportsPassword() {
        // Given
        RegisterRequest request = new RegisterRequest("lucida", "lucida@example.com", "corta", true, true);

        // When
        Set<ConstraintViolation<RegisterRequest>> errors = validator.validate(request);

        // Then
        assertEquals(Set.of("password"), fieldsOf(errors));
    }

    @Test
    void validate_termsNotAccepted_reportsTerms() {
        // Given
        RegisterRequest request = new RegisterRequest("lucida", "lucida@example.com", "una-password-lunga", true, false);

        // When
        Set<ConstraintViolation<RegisterRequest>> errors = validator.validate(request);

        // Then
        assertEquals(Set.of("termsAccepted"), fieldsOf(errors));
    }

    private static Set<String> fieldsOf(Set<ConstraintViolation<RegisterRequest>> errors) {
        return errors.stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
