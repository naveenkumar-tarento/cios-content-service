package com.igot.cios.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileLogInfoEntityTest {

    @Test
    void settersAndGetters_roundTrip() {
        JsonNode logData = new ObjectMapper().createObjectNode().put("key", "value");
        FileLogInfoEntity entity = new FileLogInfoEntity();
        entity.setId("log-1");
        entity.setFileId("file-1");
        entity.setLogData(logData);
        entity.setHasFailure(true);

        assertEquals("log-1", entity.getId());
        assertEquals("file-1", entity.getFileId());
        assertEquals(logData, entity.getLogData());
        assertTrue(entity.isHasFailure());
    }

    @Test
    void allArgsConstructor_setsFields() {
        FileLogInfoEntity entity = new FileLogInfoEntity("log-2", "file-2", null, false);
        assertEquals("log-2", entity.getId());
        assertEquals("file-2", entity.getFileId());
        assertFalse(entity.isHasFailure());
    }
}
