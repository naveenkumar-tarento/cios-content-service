package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginatedResponseTest {

    @Test
    void allArgsConstructor_setsFields() {
        List<String> result = List.of("a", "b");
        PaginatedResponse<String> response = new PaginatedResponse<>(result, 2, 10L, 2, 5, 0);

        assertEquals(result, response.getResult());
        assertEquals(2, response.getTotalPages());
        assertEquals(10L, response.getTotalElements());
        assertEquals(2, response.getNumberOfElements());
        assertEquals(5, response.getSize());
        assertEquals(0, response.getPage());
    }

    @Test
    void settersAndGetters_roundTrip() {
        PaginatedResponse<String> response = new PaginatedResponse<>();
        response.setResult(List.of("x"));
        response.setTotalPages(1);
        response.setTotalElements(1L);
        response.setNumberOfElements(1);
        response.setSize(10);
        response.setPage(0);

        assertEquals(List.of("x"), response.getResult());
        assertEquals(1, response.getTotalPages());
        assertEquals(1L, response.getTotalElements());
    }

    @Test
    void equalsAndHashCode_sameValues() {
        PaginatedResponse<String> response1 = new PaginatedResponse<>(List.of("a"), 1, 1L, 1, 10, 0);
        PaginatedResponse<String> response2 = new PaginatedResponse<>(List.of("a"), 1, 1L, 1, 10, 0);
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}
