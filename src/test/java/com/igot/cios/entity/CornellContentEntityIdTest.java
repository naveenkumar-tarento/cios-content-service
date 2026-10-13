package com.igot.cios.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CornellContentEntityIdTest {

    @Test
    void settersAndGetters_roundTrip() {
        CornellContentEntityId id = new CornellContentEntityId();
        id.setExternalId("ext-1");
        id.setPartnerId("partner-1");
        assertEquals("ext-1", id.getExternalId());
        assertEquals("partner-1", id.getPartnerId());
    }

    @Test
    void allArgsConstructor_setsFields() {
        CornellContentEntityId id = new CornellContentEntityId("ext-1", "partner-1");
        assertEquals("ext-1", id.getExternalId());
        assertEquals("partner-1", id.getPartnerId());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        CornellContentEntityId id1 = new CornellContentEntityId("ext-1", "partner-1");
        CornellContentEntityId id2 = new CornellContentEntityId("ext-1", "partner-1");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void equals_differentValues_returnsFalse() {
        CornellContentEntityId id1 = new CornellContentEntityId("ext-1", "partner-1");
        CornellContentEntityId id2 = new CornellContentEntityId("ext-2", "partner-2");
        assertNotEquals(id1, id2);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        CornellContentEntityId id1 = new CornellContentEntityId("ext-1", "partner-1");
        assertEquals(id1, id1);
    }

    @Test
    void equals_null_returnsFalse() {
        CornellContentEntityId id1 = new CornellContentEntityId("ext-1", "partner-1");
        assertNotEquals(null, id1);
    }

    @Test
    void equals_differentClass_returnsFalse() {
        CornellContentEntityId id1 = new CornellContentEntityId("ext-1", "partner-1");
        assertNotEquals("some string", id1);
    }
}
