package com.igot.cios.util.transactional.cassandrautils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CassandraPropertyReaderTest {

    @Test
    void getInstance_returnsSameSingletonInstance() {
        CassandraPropertyReader first = CassandraPropertyReader.getInstance();
        CassandraPropertyReader second = CassandraPropertyReader.getInstance();
        assertSame(first, second);
    }

    @Test
    void readProperty_unknownKey_returnsKeyItself() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        String result = reader.readProperty("definitely.unknown.property.xyz");
        assertEquals("definitely.unknown.property.xyz", result);
    }
}
