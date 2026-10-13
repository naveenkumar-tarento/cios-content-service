package com.igot.cios.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProducerConfigurationTest {

    private final ProducerConfiguration producerConfiguration = new ProducerConfiguration();

    @Test
    void producerFactory_returnsDefaultKafkaProducerFactory() {
        ReflectionTestUtils.setField(producerConfiguration, "kafkabootstrapAddress", "localhost:9092");

        ProducerFactory<String, String> factory = producerConfiguration.producerFactory();

        assertNotNull(factory);
    }

    @Test
    void kafkaTemplate_wrapsProducerFactory() {
        ReflectionTestUtils.setField(producerConfiguration, "kafkabootstrapAddress", "localhost:9092");

        KafkaTemplate<String, String> template = producerConfiguration.kafkaTemplate();

        assertNotNull(template);
    }
}
