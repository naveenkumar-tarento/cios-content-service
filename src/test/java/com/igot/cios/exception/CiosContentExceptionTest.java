package com.igot.cios.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CiosContentExceptionTest {

    @Test
    void noArgConstructor_leavesFieldsNull() {
        CiosContentException ex = new CiosContentException();
        assertNull(ex.getCode());
        assertNull(ex.getMessage());
        assertNull(ex.getHttpStatusCode());
        assertNull(ex.getErrors());
    }

    @Test
    void codeAndMessageConstructor_setsFields() {
        CiosContentException ex = new CiosContentException("ERR_CODE", "something failed");
        assertEquals("ERR_CODE", ex.getCode());
        assertEquals("something failed", ex.getMessage());
    }

    @Test
    void codeMessageStatusConstructor_setsFields() {
        CiosContentException ex = new CiosContentException("ERR_CODE", "failed", HttpStatus.BAD_REQUEST);
        assertEquals("ERR_CODE", ex.getCode());
        assertEquals("failed", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    void messageAndStatusConstructor_setsFields() {
        CiosContentException ex = new CiosContentException("failed", HttpStatus.INTERNAL_SERVER_ERROR);
        assertNull(ex.getCode());
        assertEquals("failed", ex.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatusCode());
    }

    @Test
    void errorsMapConstructor_setsMessageFromErrors() {
        Map<String, String> errors = Map.of("field", "must not be blank");
        CiosContentException ex = new CiosContentException(errors);
        assertEquals(errors, ex.getErrors());
        assertEquals(errors.toString(), ex.getMessage());
    }
}
