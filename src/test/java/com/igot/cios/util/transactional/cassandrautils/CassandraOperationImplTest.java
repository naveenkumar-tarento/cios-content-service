package com.igot.cios.util.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.igot.cios.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private CqlSession session;

    private CassandraOperationImpl cassandraOperation;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        cassandraOperation = new CassandraOperationImpl(connectionManager);
    }

    @Test
    void insertRecord_success_returnsSuccessResponse() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        BoundStatement boundStatement = mock(BoundStatement.class);
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("id", "1");
        request.put("name", "test");

        Object response = cassandraOperation.insertRecord("keyspace1", "table1", request);

        assertNotNull(response);
        verify(session).execute(boundStatement);
    }

    @Test
    void insertRecord_exceptionThrown_returnsFailedResponse() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        when(session.prepare(anyString())).thenThrow(new RuntimeException("prepare failed"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("id", "1");

        Object response = cassandraOperation.insertRecord("keyspace1", "table1", request);

        assertNotNull(response);
    }

    @Test
    void updateRecord_success_returnsSuccessResponse() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        ResultSet resultSet = mock(ResultSet.class);
        when(session.execute(any(Statement.class))).thenReturn(resultSet);

        Map<String, Object> updateAttributes = new LinkedHashMap<>();
        updateAttributes.put("status", "done");
        Map<String, Object> compositeKey = new LinkedHashMap<>();
        compositeKey.put("id", "1");

        Map<String, Object> response = cassandraOperation.updateRecord("keyspace1", "table1", updateAttributes, compositeKey);

        assertEquals(Constants.SUCCESS, response.get(Constants.RESPONSE));
    }

    @Test
    void updateRecord_exceptionThrown_rethrowsAndPopulatesFailedResponse() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("update failed"));

        Map<String, Object> updateAttributes = new LinkedHashMap<>();
        updateAttributes.put("status", "done");
        Map<String, Object> compositeKey = new LinkedHashMap<>();
        compositeKey.put("id", "1");

        assertThrows(RuntimeException.class, () ->
                cassandraOperation.updateRecord("keyspace1", "table1", updateAttributes, compositeKey));
    }

    @Test
    void getRecordsByProperties_success_returnsMappedRows() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        ResultSet resultSet = buildSingleRowResultSet("status", "live");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        Map<String, Object> propertyMap = new LinkedHashMap<>();
        propertyMap.put("partnerCode", "partner1");
        propertyMap.put("ids", List.of("a", "b"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace1", "table1", propertyMap, List.of("status"));

        assertEquals(1, result.size());
        assertEquals("live", result.get(0).get("status"));
    }

    @Test
    void getRecordsByProperties_exceptionThrown_returnsEmptyList() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("query failed"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace1", "table1", null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_success_withLimit() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        ResultSet resultSet = buildSingleRowResultSet("status", "done");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        List<Map<String, Object>> result = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                "keyspace1", "table1", Map.of(), List.of("status"), 10);

        assertEquals(1, result.size());
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_exceptionThrown_returnsEmptyList() {
        when(connectionManager.getSession("keyspace1")).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("query failed"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                "keyspace1", "table1", Map.of(), null, null);

        assertTrue(result.isEmpty());
    }

    private ResultSet buildSingleRowResultSet(String columnName, Object columnValue) {
        ResultSet resultSet = mock(ResultSet.class);
        ColumnDefinitions columnDefinitions = mock(ColumnDefinitions.class);
        ColumnDefinition columnDefinition = mock(ColumnDefinition.class);
        CqlIdentifier identifier = CqlIdentifier.fromInternal(columnName);
        when(columnDefinition.getName()).thenReturn(identifier);
        List<ColumnDefinition> definitionList = List.of(columnDefinition);
        doAnswer(invocation -> {
            definitionList.forEach(invocation.getArgument(0));
            return null;
        }).when(columnDefinitions).forEach(any());
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);

        Row row = mock(Row.class);
        when(row.getObject(columnName)).thenReturn(columnValue);
        List<Row> rows = List.of(row);
        when(resultSet.iterator()).thenReturn(rows.iterator());
        return resultSet;
    }
}
