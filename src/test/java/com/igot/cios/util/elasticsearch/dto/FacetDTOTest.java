package com.igot.cios.util.elasticsearch.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacetDTOTest {

    @Test
    void settersAndGetters_roundTrip() {
        FacetDTO facet = new FacetDTO();
        facet.setValue("india");
        facet.setCount(42L);
        assertEquals("india", facet.getValue());
        assertEquals(42L, facet.getCount());
    }

    @Test
    void allArgsConstructor_setsFields() {
        FacetDTO facet = new FacetDTO("usa", 10L);
        assertEquals("usa", facet.getValue());
        assertEquals(10L, facet.getCount());
    }
}
