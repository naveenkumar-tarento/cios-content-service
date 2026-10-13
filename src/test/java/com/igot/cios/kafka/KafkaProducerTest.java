package com.igot.cios.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void push_success_sendsSerializedMessage() throws Exception {
        KafkaProducer producer = new KafkaProducer(kafkaTemplate, objectMapper);
        Object payload = new Object();
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"key\":\"value\"}");

        producer.push("test-topic", payload);

        verify(kafkaTemplate).send("test-topic", "{\"key\":\"value\"}");
    }

    @Test
    void push_serializationFails_throwsResponseStatusException() throws Exception {
        KafkaProducer producer = new KafkaProducer(kafkaTemplate, objectMapper);
        Object payload = new Object();
        when(objectMapper.writeValueAsString(payload)).thenThrow(new RuntimeException("serialization failed"));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> producer.push("test-topic", payload));
        assertEquals("serialization failed", thrown.getReason());
    }
}
