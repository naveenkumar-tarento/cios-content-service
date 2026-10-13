package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void equalsHashCodeAndToString_lombokGenerated() {
        LogStatus status1 = new LogStatus();
        LogStatus status2 = new LogStatus();

        assertEquals(status1, status2);
        assertEquals(status1.hashCode(), status2.hashCode());
        assertNotNull(status1.toString());

        status2.setHasFailures(true);
        assertNotEquals(status1, status2);
    }
}
