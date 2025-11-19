package com.contentaggregation.metrics;

import com.contentaggregation.metrics.config.EventProcessingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(EventProcessingProperties.class)
public class MetricsServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        SpringApplication.run(MetricsServiceApplication.class, args);
    }
}

