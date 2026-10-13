package com.igot.cios.util.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.util.elasticsearch.dto.SearchCriteria;
import com.igot.cios.util.elasticsearch.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EsUtilServiceImplTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    private EsUtilServiceImpl esUtilService;
    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setup() {
        realObjectMapper = new ObjectMapper();
        esUtilService = new EsUtilServiceImpl(elasticsearchClient, realObjectMapper);
    }

    @Test
    void addDocument_unresolvableSchemaPath_returnsNull() {
        Map<String, Object> document = new HashMap<>();
        document.put("field1", "value1");

        String result = esUtilService.addDocument("index1", "doc1", document, "/non-existent-schema.json");

        assertNull(result);
    }

    @Test
    void updateDocument_unresolvableSchemaPath_propagatesIllegalArgumentException() {
        // updateDocument only catches IOException; a null schema InputStream makes objectMapper.readValue
        // throw IllegalArgumentException, which is not caught - this documents existing behavior as-is.
        assertThrows(IllegalArgumentException.class, () ->
                esUtilService.updateDocument("index1", "doc1", new HashMap<>(), "/non-existent-schema.json"));
    }

    @Test
    void deleteDocument_deletedSuccessfully_refreshesIndex() throws Exception {
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.result()).thenReturn(Result.Deleted);
        when(elasticsearchClient.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);

        esUtilService.deleteDocument("doc1", "index1");

        verify(indicesClient).refresh(any(co.elastic.clients.elasticsearch.indices.RefreshRequest.class));
    }

    @Test
    void deleteDocument_notFound_logsErrorWithoutRefresh() throws Exception {
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.result()).thenReturn(Result.NotFound);
        when(elasticsearchClient.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);

        esUtilService.deleteDocument("doc1", "index1");

        verify(elasticsearchClient, never()).indices();
    }

    @Test
    void deleteDocument_exceptionThrown_doesNotPropagate() throws Exception {
        when(elasticsearchClient.delete(any(DeleteRequest.class))).thenThrow(new RuntimeException("es down"));

        assertDoesNotThrow(() -> esUtilService.deleteDocument("doc1", "index1"));
    }

    @Test
    void isIndexPresent_success_returnsTrue() throws Exception {
        GetIndexResponse response = mock(GetIndexResponse.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.get(any(co.elastic.clients.elasticsearch.indices.GetIndexRequest.class))).thenReturn(response);

        assertTrue(esUtilService.isIndexPresent("index1"));
    }

    @Test
    void isIndexPresent_ioException_returnsFalse() throws Exception {
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.get(any(co.elastic.clients.elasticsearch.indices.GetIndexRequest.class)))
                .thenThrow(new java.io.IOException("not found"));

        assertFalse(esUtilService.isIndexPresent("index1"));
    }

    @Test
    void saveAll_success_returnsBulkResponse() throws Exception {
        JsonNode entity = realObjectMapper.createObjectNode().put("id", "doc1");
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        BulkResponse result = esUtilService.saveAll("index1", List.of(entity));

        assertSame(bulkResponse, result);
    }

    @Test
    void saveAll_exceptionThrown_wrapsAsCiosContentException() throws Exception {
        JsonNode entity = realObjectMapper.createObjectNode().put("id", "doc1");
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenThrow(new RuntimeException("bulk failed"));

        assertThrows(CiosContentException.class, () -> esUtilService.saveAll("index1", List.of(entity)));
    }

    @Test
    void deleteDocumentsByCriteria_noMatches_logsNoDocuments() throws Exception {
        SearchResponse<Object> emptyResponse = buildSearchResponse(0, Collections.emptyMap());
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(emptyResponse);

        assertDoesNotThrow(() -> esUtilService.deleteDocumentsByCriteria("index1", Query.of(q -> q.matchAll(m -> m))));

        verify(elasticsearchClient, never()).bulk(any(BulkRequest.class));
    }

    @Test
    void deleteDocumentsByCriteria_withMatches_deletesThem() throws Exception {
        SearchResponse<Object> responseWithHits = buildSearchResponseWithOneHit();
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(responseWithHits);
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        assertDoesNotThrow(() -> esUtilService.deleteDocumentsByCriteria("index1", Query.of(q -> q.matchAll(m -> m))));

        verify(elasticsearchClient).bulk(any(BulkRequest.class));
    }

    @Test
    void deleteDocumentsByCriteria_exceptionThrown_doesNotPropagate() throws Exception {
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenThrow(new RuntimeException("search failed"));

        assertDoesNotThrow(() -> esUtilService.deleteDocumentsByCriteria("index1", Query.of(q -> q.matchAll(m -> m))));
    }

    @Test
    void searchDocuments_basicFilters_buildsAndExecutesQuery() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        filterMap.put("isActive", true);
        filterMap.put("partnerCode", "partner1");
        filterMap.put("tags", new ArrayList<>(List.of("java", "spring")));
        criteria.setFilterCriteriaMap(filterMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);
        criteria.setOrderBy("name");
        criteria.setOrderDirection("asc");
        criteria.setSearchString("test");
        criteria.setFacets(List.of("category"));
        criteria.setRequestedFields(List.of("name", "id"));

        SearchResponse<Object> response = buildSearchResponse(0, Map.of("category_agg", buildStringTermsAggregate()));
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        SearchResult result = esUtilService.searchDocuments("index1", criteria);

        assertNotNull(result);
        assertEquals(0L, result.getTotalCount());
        assertTrue(result.getFacets().containsKey("category"));
        assertEquals(1, result.getFacets().get("category").size());
        assertEquals("books", result.getFacets().get("category").get(0).getValue());
    }

    @Test
    void searchDocuments_mustNotAndNestedMapFilters_buildsQuery() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        filterMap.put("must_not", new ArrayList<>(List.of("draft")));
        Map<String, Object> nested = new HashMap<>();
        nested.put("isPublished", true);
        filterMap.put("content", nested);
        criteria.setFilterCriteriaMap(filterMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(0);
        criteria.setRequestedFields(Collections.emptyList());

        SearchResponse<Object> response = buildSearchResponse(0, Collections.emptyMap());
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        SearchResult result = esUtilService.searchDocuments("index1", criteria);

        assertNotNull(result);
    }

    @Test
    void searchDocuments_nestedStringFilter_propagatesClassCastException() {
        // applyNestedFieldQuery's String branch also casts a List<FieldValue> directly to
        // TermsQueryField, which can never succeed - this documents existing behavior as-is.
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("status", "live");
        filterMap.put("content", nested);
        criteria.setFilterCriteriaMap(filterMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        assertThrows(ClassCastException.class, () -> esUtilService.searchDocuments("index1", criteria));
    }

    @Test
    void searchDocuments_nestedArrayListFilter_propagatesClassCastException() {
        // applyNestedFieldQuery's ArrayList branch casts the value directly to TermsQueryField,
        // which can never succeed for a real ArrayList - this documents existing behavior as-is.
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("flags", new ArrayList<>(List.of("a", "b")));
        filterMap.put("content", nested);
        criteria.setFilterCriteriaMap(filterMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        assertThrows(ClassCastException.class, () -> esUtilService.searchDocuments("index1", criteria));
    }

    @Test
    void searchDocuments_rangeFilter_buildsRangeQuery() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        HashMap<String, Object> filterMap = new HashMap<>();
        Map<String, Object> rangeCondition = new HashMap<>();
        rangeCondition.put(">=", JsonData.of(10));
        rangeCondition.put("<=", JsonData.of(100));
        rangeCondition.put(">", JsonData.of(5));
        rangeCondition.put("<", JsonData.of(200));
        filterMap.put("score", rangeCondition);
        criteria.setFilterCriteriaMap(filterMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        SearchResponse<Object> response = buildSearchResponse(0, Collections.emptyMap());
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        assertDoesNotThrow(() -> esUtilService.searchDocuments("index1", criteria));
    }

    @Test
    void searchDocuments_withBoolQueryPart_buildsNestedBoolQuery() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        Map<String, Object> termMap = new HashMap<>();
        termMap.put("status", FieldValue.of("live"));
        Map<String, Object> mustClause = new HashMap<>();
        mustClause.put("term", termMap);

        Map<String, Object> boolMap = new HashMap<>();
        boolMap.put("must", new ArrayList<>(List.of(mustClause)));
        boolMap.put("filter", new ArrayList<>(List.of(mustClause)));
        boolMap.put("must_not", new ArrayList<>(List.of(mustClause)));
        boolMap.put("should", new ArrayList<>(List.of(mustClause)));

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("bool", boolMap);
        criteria.setQuery(queryMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        SearchResponse<Object> response = buildSearchResponse(0, Collections.emptyMap());
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        assertDoesNotThrow(() -> esUtilService.searchDocuments("index1", criteria));
    }

    @Test
    void searchDocuments_withTermsMatchAndRangeQueryParts_executesEachBranch() throws Exception {
        SearchResponse<Object> response = buildSearchResponse(0, Collections.emptyMap());
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class))).thenReturn(response);

        SearchCriteria termsCriteria = new SearchCriteria();
        Map<String, Object> termsValue = new HashMap<>();
        termsValue.put("status", co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField.of(
                t -> t.value(List.of(FieldValue.of("live")))));
        Map<String, Object> termsQuery = new HashMap<>();
        termsQuery.put("terms", termsValue);
        termsCriteria.setQuery(termsQuery);
        termsCriteria.setPageNumber(0);
        termsCriteria.setPageSize(10);
        assertDoesNotThrow(() -> esUtilService.searchDocuments("index1", termsCriteria));

        SearchCriteria matchCriteria = new SearchCriteria();
        Map<String, Object> matchValue = new HashMap<>();
        matchValue.put("name", FieldValue.of("java"));
        Map<String, Object> matchQuery = new HashMap<>();
        matchQuery.put("match", matchValue);
        matchCriteria.setQuery(matchQuery);
        matchCriteria.setPageNumber(0);
        matchCriteria.setPageSize(10);
        assertDoesNotThrow(() -> esUtilService.searchDocuments("index1", matchCriteria));

        SearchCriteria rangeCriteria = new SearchCriteria();
        Map<String, Object> rangeConditions = new HashMap<>();
        rangeConditions.put("gt", 1);
        rangeConditions.put("gte", 1);
        rangeConditions.put("lt", 100);
        rangeConditions.put("lte", 100);
        Map<String, Object> rangeValue = new HashMap<>();
        rangeValue.put("score", rangeConditions);
        Map<String, Object> rangeQuery = new HashMap<>();
        rangeQuery.put("range", rangeValue);
        rangeCriteria.setQuery(rangeQuery);
        rangeCriteria.setPageNumber(0);
        rangeCriteria.setPageSize(10);
        assertDoesNotThrow(() -> esUtilService.searchDocuments("index1", rangeCriteria));
    }

    @Test
    void searchDocuments_unsupportedQueryType_throwsIllegalArgument() {
        SearchCriteria criteria = new SearchCriteria();
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("unsupported_type", Map.of());
        criteria.setQuery(queryMap);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        assertThrows(IllegalArgumentException.class, () -> esUtilService.searchDocuments("index1", criteria));
    }

    @Test
    void searchDocuments_ioExceptionFromClient_returnsNull() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
                .thenThrow(new java.io.IOException("es down"));

        SearchResult result = esUtilService.searchDocuments("index1", criteria);

        assertNull(result);
    }

    private SearchResponse<Object> buildSearchResponse(long total, Map<String, Aggregate> aggregations) {
        return SearchResponse.of(r -> r
                .took(1)
                .timedOut(false)
                .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
                .hits(h -> h
                        .total(new TotalHits.Builder().value(total).relation(TotalHitsRelation.Eq).build())
                        .hits(Collections.emptyList())
                )
                .aggregations(aggregations)
        );
    }

    private SearchResponse<Object> buildSearchResponseWithOneHit() {
        return SearchResponse.of(r -> r
                .took(1)
                .timedOut(false)
                .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
                .hits(h -> h
                        .total(new TotalHits.Builder().value(1).relation(TotalHitsRelation.Eq).build())
                        .hits(List.of(co.elastic.clients.elasticsearch.core.search.Hit.of(hb -> hb
                                .index("index1").id("doc1").score(1.0))))
                )
        );
    }

    private Aggregate buildStringTermsAggregate() {
        StringTermsBucket bucket = StringTermsBucket.of(b -> b.key(FieldValue.of("books")).docCount(5));
        StringTermsAggregate sterms = StringTermsAggregate.of(s -> s.buckets(bb -> bb.array(List.of(bucket))));
        return Aggregate.of(a -> a.sterms(sterms));
    }

}
