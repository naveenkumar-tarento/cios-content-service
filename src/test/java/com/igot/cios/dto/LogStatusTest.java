package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogStatusTest {

    @Test
    void defaultConstructor_initializesEmptyState() {
        LogStatus status = new LogStatus();
        assertFalse(status.isHasFailures());
        assertTrue(status.getSuccessLogs().isEmpty());
        assertTrue(status.getErrorLogs().isEmpty());
    }

    @Test
    void clearLogs_resetsState() {
        LogStatus status = new LogStatus();
        LinkedHashMap<String, String> entry = new LinkedHashMap<>();
        entry.put("key", "value");
        status.getSuccessLogs().add(entry);
        status.getErrorLogs().add(entry);
        status.setHasFailures(true);

        status.clearLogs();

        assertTrue(status.getSuccessLogs().isEmpty());
        assertTrue(status.getErrorLogs().isEmpty());
        assertFalse(status.isHasFailures());
    }
}
