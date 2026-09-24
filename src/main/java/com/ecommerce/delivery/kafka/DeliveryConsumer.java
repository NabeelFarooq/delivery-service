package com.ecommerce.delivery.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.delivery.service.DeliveryProcessor;

@Component
public class DeliveryConsumer {

    private final DeliveryProcessor processor;

    public DeliveryConsumer(
            DeliveryProcessor processor) {

        this.processor = processor;
    }

    @KafkaListener(
            topics = "payment-success",
            groupId = "delivery-service-group")
    public void consume(String payload)
            throws Exception {

        processor.processPaymentSuccess(payload);
    }
}