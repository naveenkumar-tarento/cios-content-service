package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestBodyDTOTest {

    @Test
    void settersAndGetters_roundTrip() {
        RequestBodyDTO dto = new RequestBodyDTO();
        dto.setServiceCode("svc-1");
        dto.setUrlMap(Map.of("from", "2024-01-01"));
        dto.setHeaderMap(Map.of("API-Key", "key"));
        dto.setRequestBody(Map.of("field", "value"));

        assertEquals("svc-1", dto.getServiceCode());
        assertEquals(Map.of("from", "2024-01-01"), dto.getUrlMap());
        assertEquals(Map.of("API-Key", "key"), dto.getHeaderMap());
        assertEquals(Map.of("field", "value"), dto.getRequestBody());
    }

    @Test
    void allArgsConstructor_setsFields() {
        RequestBodyDTO dto = new RequestBodyDTO("svc-2", Map.of(), Map.of(), Map.of());
        assertEquals("svc-2", dto.getServiceCode());
    }

    @Test
    void equalsAndHashCode_sameValues() {
        RequestBodyDTO dto1 = new RequestBodyDTO("svc-1", Map.of(), Map.of(), Map.of());
        RequestBodyDTO dto2 = new RequestBodyDTO("svc-1", Map.of(), Map.of(), Map.of());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
