package com.contentaggregation.metrics.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
@EnableConfigurationProperties(KafkaStreamsConfig.StreamsProperties.class)
public class KafkaStreamsConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration(
        KafkaProperties kafkaProperties,
        StreamsProperties streamsProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildStreamsProperties(null));
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamsProperties.applicationId());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, streamsProperties.processingGuarantee());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, streamsProperties.stateDir());
        return new KafkaStreamsConfiguration(props);
    }

    @ConfigurationProperties(prefix = "app.kafka.streams")
    public static class StreamsProperties {
        private String applicationId = "metrics-service-streams";
        private String processingGuarantee = StreamsConfig.EXACTLY_ONCE_V2;
        private String stateDir = "/tmp/kafka-streams";

        public String applicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String processingGuarantee() {
            return processingGuarantee;
        }

        public void setProcessingGuarantee(String processingGuarantee) {
            this.processingGuarantee = processingGuarantee;
        }

        public String stateDir() {
            return stateDir;
        }

        public void setStateDir(String stateDir) {
            this.stateDir = stateDir;
        }
    }
}

