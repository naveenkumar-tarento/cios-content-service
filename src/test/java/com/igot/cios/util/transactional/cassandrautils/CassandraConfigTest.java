package com.igot.cios.util.transactional.cassandrautils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CassandraConfigTest {

    private final CassandraConfig config = new CassandraConfig() {
    };

    @Test
    void contactPoints_roundTrip() {
        config.setContactPoints("127.0.0.1");
        assertEquals("127.0.0.1", config.getContactPoints());
    }

    @Test
    void port_roundTrip() {
        config.setPort(9042);
        assertEquals(9042, config.getPort());
    }

    @Test
    void keyspaceName_roundTrip() {
        config.setKeyspaceName("sunbird");
        assertEquals("sunbird", config.getKeyspaceName());
    }
}
