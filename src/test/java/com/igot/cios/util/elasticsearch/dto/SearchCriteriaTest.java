package com.igot.cios.util.elasticsearch.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchCriteriaTest {

    @Test
    void settersAndGetters_roundTrip() {
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        filterMap.put("status", "live");
        Map<String, Object> query = new HashMap<>();
        query.put("term", "java");

        criteria.setFilterCriteriaMap(filterMap);
        criteria.setRequestedFields(List.of("name", "id"));
        criteria.setPageNumber(0);
        criteria.setPageSize(10);
        criteria.setOrderBy("name");
        criteria.setOrderDirection("asc");
        criteria.setSearchString("java");
        criteria.setFacets(List.of("category"));
        criteria.setQuery(query);

        assertEquals(filterMap, criteria.getFilterCriteriaMap());
        assertEquals(List.of("name", "id"), criteria.getRequestedFields());
        assertEquals(0, criteria.getPageNumber());
        assertEquals(10, criteria.getPageSize());
        assertEquals("name", criteria.getOrderBy());
        assertEquals("asc", criteria.getOrderDirection());
        assertEquals("java", criteria.getSearchString());
        assertEquals(List.of("category"), criteria.getFacets());
        assertEquals(query, criteria.getQuery());
    }

    @Test
    void allArgsConstructor_setsFields() {
        HashMap<String, Object> filterMap = new HashMap<>();
        SearchCriteria criteria = new SearchCriteria(filterMap, List.of("id"), 1, 20, "id", "desc", "term", List.of("tag"), new HashMap<>());
        assertEquals(1, criteria.getPageNumber());
        assertEquals(20, criteria.getPageSize());
    }
}
