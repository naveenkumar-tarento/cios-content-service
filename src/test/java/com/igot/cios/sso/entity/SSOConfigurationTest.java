package com.igot.cios.sso.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SSOConfigurationTest {

    @Test
    void allArgsConstructor_setsAllFields() {
        JsonNode ssoData = new ObjectMapper().createObjectNode().put("clientId", "c1");
        Timestamp created = new Timestamp(System.currentTimeMillis());
        Timestamp updated = new Timestamp(System.currentTimeMillis());

        SSOConfiguration config = new SSOConfiguration("partner1", ssoData, created, updated);

        assertEquals("partner1", config.getPartnerId());
        assertEquals(ssoData, config.getSsoData());
        assertEquals(created, config.getCreatedOn());
        assertEquals(updated, config.getUpdatedOn());
    }

    @Test
    void settersAndGetters_roundTrip() {
        SSOConfiguration config = new SSOConfiguration();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        config.setPartnerId("partner2");
        config.setCreatedOn(now);
        config.setUpdatedOn(now);

        assertEquals("partner2", config.getPartnerId());
        assertEquals(now, config.getCreatedOn());
        assertEquals(now, config.getUpdatedOn());
    }
}
