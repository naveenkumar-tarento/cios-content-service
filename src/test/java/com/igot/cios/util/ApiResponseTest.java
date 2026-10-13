package com.igot.cios.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void defaultConstructor_setsDefaults() {
        ApiResponse response = new ApiResponse();
        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
        assertNotNull(response.getResult());
    }

    @Test
    void idConstructor_setsId() {
        ApiResponse response = new ApiResponse("resp-1");
        assertEquals("resp-1", response.getId());
    }

    @Test
    void settersAndGetters_roundTrip() {
        ApiResponse response = new ApiResponse();
        response.setId("id-1");
        response.setVer("v2");
        response.setTs("ts-1");
        ApiRespParam params = new ApiRespParam("msg-1");
        response.setParams(params);
        response.setResponseCode(HttpStatus.OK);

        assertEquals("id-1", response.getId());
        assertEquals("v2", response.getVer());
        assertEquals("ts-1", response.getTs());
        assertEquals(params, response.getParams());
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void resultMapOperations() {
        ApiResponse response = new ApiResponse();
        response.put("key1", "value1");
        assertEquals("value1", response.get("key1"));
        assertTrue(response.containsKey("key1"));
        assertFalse(response.containsKey("missing"));

        Map<String, Object> extra = new HashMap<>();
        extra.put("key2", "value2");
        response.putAll(extra);
        assertEquals("value2", response.get("key2"));

        Map<String, Object> newResult = new HashMap<>();
        newResult.put("key3", "value3");
        response.setResult(newResult);
        assertEquals(newResult, response.getResult());
    }
}
