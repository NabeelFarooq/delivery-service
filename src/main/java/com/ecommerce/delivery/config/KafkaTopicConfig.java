package com.ecommerce.delivery.config;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic deliveryCreatedTopic() {

        return new NewTopic(
                "delivery-created",
                3,
                (short) 1);
    }

    @Bean
    public NewTopic deliveryOutForDeliveryTopic() {

        return new NewTopic(
                "delivery-out-for-delivery",
                3,
                (short) 1);
    }

    @Bean
    public NewTopic orderDeliveredTopic() {

        return new NewTopic(
                "order-delivered",
                3,
                (short) 1);
    }
}