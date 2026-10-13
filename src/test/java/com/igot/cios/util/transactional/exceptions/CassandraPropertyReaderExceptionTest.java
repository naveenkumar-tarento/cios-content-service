package com.igot.cios.util.transactional.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CassandraPropertyReaderExceptionTest {

    @Test
    void constructor_setsMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        CassandraPropertyReaderException exception =
                new CassandraPropertyReaderException("failed to read property", cause);

        assertEquals("failed to read property", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
