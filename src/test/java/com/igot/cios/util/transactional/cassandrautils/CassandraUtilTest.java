package com.igot.cios.util.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CassandraUtilTest {

    @Test
    void getPreparedStatement_buildsInsertQueryWithPlaceholders() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", "1");
        values.put("name", "test");

        String query = CassandraUtil.getPreparedStatement("keyspace1", "table1", values);

        assertEquals("INSERT INTO keyspace1.table1(id,name) VALUES (?,?);", query);
    }

    @Test
    void createResponse_withKey_groupsRowsByKeyColumn() {
        ResultSet resultSet = mock(ResultSet.class);
        ColumnDefinitions columnDefinitions = mock(ColumnDefinitions.class);
        ColumnDefinition columnDefinition = mock(ColumnDefinition.class);
        CqlIdentifier identifier = CqlIdentifier.fromInternal("status");
        when(columnDefinition.getName()).thenReturn(identifier);
        List<ColumnDefinition> definitionList = List.of(columnDefinition);
        doAnswer(invocation -> {
            definitionList.forEach(invocation.getArgument(0));
            return null;
        }).when(columnDefinitions).forEach(any());
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);

        Row row = mock(Row.class);
        when(row.getObject("status")).thenReturn("live");
        when(resultSet.iterator()).thenReturn(List.of(row).iterator());

        Map<String, Object> response = CassandraUtil.createResponse(resultSet, "status");

        assertEquals(1, response.size());
        assertTrue(response.containsKey("live"));
    }
}
