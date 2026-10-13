package com.igot.cios.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CiosRestExceptionHandlingTest {

    private final CiosRestExceptionHandling handler = new CiosRestExceptionHandling();

    @Test
    void handleException_ciosContentExceptionWithStatus_returnsBadRequestBody() {
        CiosContentException ex = new CiosContentException("ERR_CODE", "failure message", HttpStatus.CONFLICT);

        ResponseEntity<CiosErrorResponse> response = handler.handleException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("ERR_CODE", response.getBody().getCode());
        assertEquals("failure message", response.getBody().getMessage());
        assertEquals(String.valueOf(HttpStatus.CONFLICT), response.getBody().getHttpStatusCode());
    }

    @Test
    void handleException_ciosContentExceptionWithoutStatus_usesDefaultStatusCode() {
        CiosContentException ex = new CiosContentException("ERR_CODE", "failure message");

        ResponseEntity<CiosErrorResponse> response = handler.handleException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(String.valueOf(HttpStatus.BAD_REQUEST.value()), response.getBody().getHttpStatusCode());
    }

    @Test
    void handleException_genericException_returnsInternalServerError() {
        Exception ex = new IllegalStateException("unexpected");

        ResponseEntity<CiosErrorResponse> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("unexpected", response.getBody().getCode());
    }
}
