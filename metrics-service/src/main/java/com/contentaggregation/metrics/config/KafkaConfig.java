package com.contentaggregation.metrics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

@Configuration
@EnableKafka
@EnableConfigurationProperties({
    KafkaConfig.KafkaTopicProperties.class,
    KafkaConfig.ProfileListenerProperties.class
})
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    public static final String USER_EVENTS_IMPRESSIONS = "user-events-impressions";
    public static final String USER_EVENTS_ENGAGEMENT = "user-events-engagement";
    public static final String USER_EVENTS_REACTIONS = "user-events-reactions";
    public static final String USER_EVENTS_SESSIONS = "user-events-sessions";
    public static final String USER_PROFILE_UPDATES = "user-profile-updates";
    public static final String USER_PROFILE_UPDATES_DLT = "user-profile-updates-dlt";

    @Bean
    public NewTopic userEventsImpressionsTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_EVENTS_IMPRESSIONS, properties);
    }

    @Bean
    public NewTopic userEventsEngagementTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_EVENTS_ENGAGEMENT, properties);
    }

    @Bean
    public NewTopic userEventsReactionsTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_EVENTS_REACTIONS, properties);
    }

    @Bean
    public NewTopic userEventsSessionsTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_EVENTS_SESSIONS, properties);
    }

    @Bean
    public NewTopic userProfileUpdatesTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_PROFILE_UPDATES, properties);
    }

    @Bean
    public NewTopic userProfileUpdatesDltTopic(KafkaTopicProperties properties) {
        return buildTopic(USER_PROFILE_UPDATES_DLT, properties);
    }

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Kafka delivery attempt {} failed for topic {}: {}", deliveryAttempt,
                record.topic(), ex.getMessage())
        );
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> profileEventsListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory,
        DefaultErrorHandler errorHandler,
        ProfileListenerProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setBatchListener(true);
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(properties.concurrency());
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
        containerProperties.setObservationEnabled(true);
        containerProperties.setIdleBetweenPolls(properties.idleBetweenPolls().toMillis());
        containerProperties.setPollTimeout(properties.pollTimeout().toMillis());
        return factory;
    }

    private NewTopic buildTopic(String name, KafkaTopicProperties properties) {
        return TopicBuilder.name(name)
            .partitions(properties.partitions())
            .replicas(properties.replicationFactor())
            .config("retention.ms", String.valueOf(properties.retention().toMillis()))
            .config("cleanup.policy", "delete")
            .config("compression.type", properties.compression())
            .build();
    }

    @ConfigurationProperties(prefix = "app.kafka.topics")
    public static class KafkaTopicProperties {
        private final int partitions;
        private final short replicationFactor;
        private final Duration retention;
        private final String compression;

        @ConstructorBinding
        public KafkaTopicProperties(
            @Name("partitions") @DefaultValue("8") Integer partitions,
            @Name("replication-factor") @DefaultValue("1") Short replicationFactor,
            @Name("retention") @DefaultValue("P7D") Duration retention,
            @Name("compression") @DefaultValue("zstd") String compression
        ) {
            if (partitions == null || partitions < 1) {
                throw new InvalidConfigurationPropertyValueException("app.kafka.topics.partitions",
                    partitions, "Partitions must be greater than 0");
            }
            if (replicationFactor == null || replicationFactor < 1) {
                throw new InvalidConfigurationPropertyValueException("app.kafka.topics.replication-factor",
                    replicationFactor, "Replication factor must be greater than 0");
            }
            this.partitions = partitions;
            this.replicationFactor = replicationFactor;
            this.retention = retention != null ? retention : Duration.ofDays(7);
            this.compression = compression != null ? compression : "zstd";
        }

        public int partitions() {
            return partitions;
        }

        public short replicationFactor() {
            return replicationFactor;
        }

        public Duration retention() {
            return retention;
        }

        public String compression() {
            return compression;
        }
    }

    @ConfigurationProperties(prefix = "app.kafka.listeners.profile")
    public static class ProfileListenerProperties {
        private int concurrency = 3;
        private Duration pollTimeout = Duration.ofSeconds(2);
        private Duration idleBetweenPolls = Duration.ofMillis(25);

        public int concurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public Duration pollTimeout() {
            return pollTimeout;
        }

        public void setPollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout;
        }

        public Duration idleBetweenPolls() {
            return idleBetweenPolls;
        }

        public void setIdleBetweenPolls(Duration idleBetweenPolls) {
            this.idleBetweenPolls = idleBetweenPolls;
        }
    }
}

