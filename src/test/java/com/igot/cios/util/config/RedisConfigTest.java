package com.igot.cios.util.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    void redisConnectionFactory_buildsLettuceConnectionFactory() {
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);

        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        assertNotNull(factory);
        assertTrue(factory instanceof LettuceConnectionFactory);
    }

    @Test
    void redisTemplate_wiresSerializersAndConnectionFactory() {
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        RedisTemplate<String, String> template = redisConfig.redisTemplate(factory);

        assertNotNull(template);
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
    }

    @Test
    void apicall_returnsRestTemplate() {
        RestTemplate restTemplate = redisConfig.apicall();
        assertNotNull(restTemplate);
    }
}
