package com.igot.cios.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsumerConfigurationTest {

    private final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration();

    private void setupProps() {
        ReflectionTestUtils.setField(consumerConfiguration, "kafkabootstrapAddress", "localhost:9092");
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaOffsetResetValue", "earliest");
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaMaxPollInterval", 300000);
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaMaxPollRecords", 500);
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaAutoCommitInterval", 1000);
    }

    @Test
    void consumerConfigs_buildsExpectedPropertyMap() {
        setupProps();

        Map<String, Object> configs = consumerConfiguration.consumerConfigs();

        assertNotNull(configs);
        assertNotNull(configs.get("bootstrap.servers"));
    }

    @Test
    void consumerFactory_returnsDefaultKafkaConsumerFactory() {
        setupProps();

        ConsumerFactory<String, String> factory = consumerConfiguration.consumerFactory();

        assertNotNull(factory);
    }

    @Test
    void kafkaListenerContainerFactory_buildsConcurrentFactory() {
        setupProps();

        org.springframework.kafka.config.KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> factory =
                consumerConfiguration.kafkaListenerContainerFactory();

        assertNotNull(factory);
    }
}
