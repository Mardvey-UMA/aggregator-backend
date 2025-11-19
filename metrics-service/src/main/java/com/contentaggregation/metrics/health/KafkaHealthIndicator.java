package com.contentaggregation.metrics.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaProperties kafkaProperties;

    public KafkaHealthIndicator(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public Health health() {
        Properties adminProps = new Properties();
        adminProps.putAll(kafkaProperties.buildAdminProperties(null));

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId().get(3, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes().get(3, TimeUnit.SECONDS).size();
            return Health.up()
                .withDetail("clusterId", clusterId)
                .withDetail("nodes", nodeCount)
                .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}


