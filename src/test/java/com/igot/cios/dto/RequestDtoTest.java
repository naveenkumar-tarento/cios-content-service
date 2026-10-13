package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDtoTest {

    @Test
    void allArgsConstructor_setsFields() {
        RequestDto dto = new RequestDto("partner-1", 10, 0, true, "keyword");

        assertEquals("partner-1", dto.getPartnerCode());
        assertEquals(10, dto.getSize());
        assertEquals(0, dto.getPage());
        assertTrue(dto.getIsActive());
        assertEquals("keyword", dto.getKeyword());
    }

    @Test
    void settersAndGetters_roundTrip() {
        RequestDto dto = new RequestDto();
        dto.setPartnerCode("partner-2");
        dto.setSize(5);
        dto.setPage(1);
        dto.setIsActive(false);
        dto.setKeyword("term");

        assertEquals("partner-2", dto.getPartnerCode());
        assertEquals(5, dto.getSize());
        assertEquals(1, dto.getPage());
        assertEquals(false, dto.getIsActive());
        assertEquals("term", dto.getKeyword());
    }

    @Test
    void equalsAndHashCode_sameValues() {
        RequestDto dto1 = new RequestDto("partner-1", 10, 0, true, "keyword");
        RequestDto dto2 = new RequestDto("partner-1", 10, 0, true, "keyword");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
