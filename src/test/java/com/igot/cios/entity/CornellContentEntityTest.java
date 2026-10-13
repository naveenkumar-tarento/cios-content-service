package com.igot.cios.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CornellContentEntityTest {

    @Test
    void allArgsConstructor_setsAllFields() {
        JsonNode ciosData = new ObjectMapper().createObjectNode().put("name", "course1");
        JsonNode sourceData = new ObjectMapper().createObjectNode().put("raw", "data");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        CornellContentEntity entity = new CornellContentEntity(
                "ext1", "partner1", ciosData, true, now, now, sourceData, "file1", "partnerCode1");

        assertEquals("ext1", entity.getExternalId());
        assertEquals("partner1", entity.getPartnerId());
        assertEquals(ciosData, entity.getCiosData());
        assertTrue(entity.getIsActive());
        assertEquals(now, entity.getCreatedDate());
        assertEquals(now, entity.getUpdatedDate());
        assertEquals(sourceData, entity.getSourceData());
        assertEquals("file1", entity.getFileId());
        assertEquals("partnerCode1", entity.getPartnerCode());
    }
}
