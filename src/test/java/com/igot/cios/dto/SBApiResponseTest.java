package com.igot.cios.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SBApiResponseTest {

    @Test
    void createDefaultResponse_setsDefaults() {
        SBApiResponse response = SBApiResponse.createDefaultResponse("api.test");

        assertEquals("api.test", response.getId());
        assertNotNull(response.getParams());
        assertEquals("success", response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getTs());
    }

    @Test
    void resultMapOperations_roundTrip() {
        SBApiResponse response = new SBApiResponse();
        response.put("key1", "value1");

        assertEquals("value1", response.get("key1"));
        assertTrue(response.containsKey("key1"));
        assertFalse(response.containsKey("missing"));

        Map<String, Object> extra = new HashMap<>();
        extra.put("key2", "value2");
        response.putAll(extra);
        assertEquals("value2", response.get("key2"));
    }

    @Test
    void settersAndGetters_roundTrip() {
        SBApiResponse response = new SBApiResponse();
        response.setId("id1");
        response.setVer("v2");
        response.setTs("ts1");
        ApiRespParam params = new ApiRespParam("msg1");
        response.setParams(params);
        response.setResponseCode(HttpStatus.CREATED);

        assertEquals("id1", response.getId());
        assertEquals("v2", response.getVer());
        assertEquals("ts1", response.getTs());
        assertEquals(params, response.getParams());
        assertEquals(HttpStatus.CREATED, response.getResponseCode());
    }
}
