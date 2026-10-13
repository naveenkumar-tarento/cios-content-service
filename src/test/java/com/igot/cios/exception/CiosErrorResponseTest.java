package com.igot.cios.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CiosErrorResponseTest {

    @Test
    void builder_setsAllFields() {
        Map<String, String> errors = Map.of("field", "must not be blank");
        CiosErrorResponse response = CiosErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Invalid payload")
                .errors(errors)
                .httpStatusCode("400")
                .build();

        assertEquals("VALIDATION_ERROR", response.getCode());
        assertEquals("Invalid payload", response.getMessage());
        assertEquals(errors, response.getErrors());
        assertEquals("400", response.getHttpStatusCode());
    }

    @Test
    void equalsAndHashCode_sameValues() {
        CiosErrorResponse response1 = CiosErrorResponse.builder().code("A").message("m").build();
        CiosErrorResponse response2 = CiosErrorResponse.builder().code("A").message("m").build();
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}
