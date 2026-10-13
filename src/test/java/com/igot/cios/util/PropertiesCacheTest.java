package com.igot.cios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PropertiesCacheTest {

    @Test
    void getInstance_returnsSameSingletonInstance() {
        PropertiesCache first = PropertiesCache.getInstance();
        PropertiesCache second = PropertiesCache.getInstance();
        assertSame(first, second);
    }

    @Test
    void getProperty_unknownKey_returnsKeyItself() {
        PropertiesCache cache = PropertiesCache.getInstance();
        String result = cache.getProperty("definitely.unknown.property.key.xyz");
        assertEquals("definitely.unknown.property.key.xyz", result);
    }

    @Test
    void readProperty_unknownKey_returnsNull() {
        PropertiesCache cache = PropertiesCache.getInstance();
        String result = cache.readProperty("definitely.unknown.property.key.xyz");
        assertNotNull(cache);
        // readProperty falls back to the raw Properties lookup (no key-itself fallback)
        assertEquals(null, result);
    }
}
