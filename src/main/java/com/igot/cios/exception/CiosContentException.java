package com.igot.cios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class CiosContentException extends RuntimeException {
    private final String code;
    private final String message;
    private final HttpStatus httpStatusCode;
    private final Map<String, String> errors;

    public CiosContentException() {
        this(null, null, null, null);
    }

    public CiosContentException(String code, String message) {
        this(code, message, null, null);
    }

    public CiosContentException(String code, String message, HttpStatus httpStatusCode) {
        this(code, message, httpStatusCode, null);
    }

    public CiosContentException(String message, HttpStatus httpStatusCode) {
        this(null, message, httpStatusCode, null);
    }

    public CiosContentException(Map<String, String> errors) {
        this(null, errors.toString(), null, errors);
    }

    private CiosContentException(String code, String message, HttpStatus httpStatusCode, Map<String, String> errors) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
        this.errors = errors;
    }

}