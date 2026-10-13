package com.igot.cios.util.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheService cacheService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        cacheService = new CacheService(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(cacheService, "cacheTtl", 300L);
    }

    @Test
    void putCache_success_writesSerializedValue() throws Exception {
        when(objectMapper.writeValueAsString("value1")).thenReturn("\"value1\"");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.putCache("key1", "value1");

        verify(valueOperations).set("key1", "\"value1\"", 300L, TimeUnit.SECONDS);
    }

    @Test
    void putCache_serializationFails_logsAndSwallowsException() throws Exception {
        when(objectMapper.writeValueAsString("value1")).thenThrow(new RuntimeException("serialize failed"));

        cacheService.putCache("key1", "value1");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void getCache_success_returnsValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn("cached-value");

        String result = cacheService.getCache("key1");

        assertEquals("cached-value", result);
    }

    @Test
    void getCache_exception_returnsNull() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        String result = cacheService.getCache("key1");

        assertNull(result);
    }

    @Test
    void deleteCache_keyFound_logsSuccessButReturnsNullByDesign() {
        when(redisTemplate.delete("key1")).thenReturn(true);

        Boolean result = cacheService.deleteCache("key1");

        assertNull(result);
    }

    @Test
    void deleteCache_keyNotFound_returnsNullByDesign() {
        when(redisTemplate.delete("key1")).thenReturn(false);

        Boolean result = cacheService.deleteCache("key1");

        assertNull(result);
    }
}
