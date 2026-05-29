package com.br.psyke.psyke.config;

import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.SessionEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerProps() {
        return Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
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