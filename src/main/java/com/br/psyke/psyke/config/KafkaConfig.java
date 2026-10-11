package com.br.psyke.psyke.config;

import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.SessionEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Only active with {@code psyke.messaging.kafka.enabled=true}. Without Kafka, domain events are
 * delivered in-process after commit (see {@code InProcessEventListeners}), which is what the
 * single-instance cloud deployment uses.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "psyke.messaging.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerProps() {
        return Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000
        );
    }

    @Bean
    public KafkaTemplate<String, AppointmentEvent> appointmentKafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }

    @Bean
    public KafkaTemplate<String, SessionEvent> sessionKafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }
}
