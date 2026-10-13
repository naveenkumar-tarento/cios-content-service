package com.igot.cios.util.elasticsearch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchResultTest {

    @Test
    void settersAndGetters_roundTrip() {
        JsonNode data = new ObjectMapper().createObjectNode().put("key", "value");
        Map<String, List<FacetDTO>> facets = Map.of("category", List.of(new FacetDTO("books", 5L)));

        SearchResult result = new SearchResult();
        result.setData(data);
        result.setFacets(facets);
        result.setTotalCount(100L);

        assertEquals(data, result.getData());
        assertEquals(facets, result.getFacets());
        assertEquals(100L, result.getTotalCount());
    }

    @Test
    void allArgsConstructor_setsFields() {
        SearchResult result = new SearchResult(null, Map.of(), 5L);
        assertEquals(5L, result.getTotalCount());
    }
}
