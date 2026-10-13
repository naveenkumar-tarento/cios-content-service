package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteContentRequestDtoTest {

    @Test
    void allArgsConstructor_setsFields() {
        List<String> externalIds = List.of("ext1", "ext2");

        DeleteContentRequestDto dto = new DeleteContentRequestDto(externalIds, "partner1");

        assertEquals(externalIds, dto.getExternalId());
        assertEquals("partner1", dto.getPartnerCode());
    }

    @Test
    void settersAndGetters_roundTrip() {
        DeleteContentRequestDto dto = new DeleteContentRequestDto();
        dto.setExternalId(List.of("ext3"));
        dto.setPartnerCode("partner2");

        assertEquals(List.of("ext3"), dto.getExternalId());
        assertEquals("partner2", dto.getPartnerCode());
    }
}
