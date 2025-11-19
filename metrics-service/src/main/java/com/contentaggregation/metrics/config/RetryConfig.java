package com.contentaggregation.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate profileRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(200);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(2000);
        template.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, Collections.singletonMap(Exception.class, true));
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}

