package com.ecommerce.delivery.service;

import java.time.LocalDateTime;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import com.ecommerce.delivery.entity.DeliveryEntity;
import com.ecommerce.delivery.entity.DeliveryStatus;
import com.ecommerce.delivery.entity.OutboxEvent;
import com.ecommerce.delivery.entity.ProcessedEventEntity;

import com.ecommerce.delivery.event.DeliveryCreatedEvent;
import com.ecommerce.delivery.event.DeliveryStatusEvent;
import com.ecommerce.delivery.event.PaymentSuccessEvent;

import com.ecommerce.delivery.repository.DeliveryRepository;
import com.ecommerce.delivery.repository.OutboxEventRepository;
import com.ecommerce.delivery.repository.ProcessedEventRepository;

import com.ecommerce.delivery.util.DeliveryEventIdGenerator;

@Service
public class DeliveryProcessor {

    private final ObjectMapper objectMapper;

    private final DeliveryRepository deliveryRepository;

    private final ProcessedEventRepository
            processedEventRepository;

    private final OutboxEventRepository
            outboxRepository;

    private final DeliveryEventIdGenerator
            idGenerator;

    public DeliveryProcessor(
            ObjectMapper objectMapper,
            DeliveryRepository deliveryRepository,
            ProcessedEventRepository processedEventRepository,
            OutboxEventRepository outboxRepository,
            DeliveryEventIdGenerator idGenerator) {

        this.objectMapper = objectMapper;

        this.deliveryRepository =
                deliveryRepository;

        this.processedEventRepository =
                processedEventRepository;

        this.outboxRepository =
                outboxRepository;

        this.idGenerator =
                idGenerator;
    }

    @Transactional
    public void processPaymentSuccess(
            String payload) throws Exception {

        PaymentSuccessEvent payment =
                objectMapper.readValue(
                        payload,
                        PaymentSuccessEvent.class);

        if (payment.getEventId() == null
                || payment.getEventId().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing eventId");
        }

        if (payment.getOrderId() == null
                || payment.getOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing orderId");
        }

        // Idempotency
        if (processedEventRepository
                .existsById(payment.getEventId())) {

            System.out.println(
                    "Duplicate payment event ignored: "
                    + payment.getEventId());

            return;
        }

        // Safety check
        if (deliveryRepository
                .findByOrderId(payment.getOrderId())
                .isPresent()) {

            processedEventRepository.save(
                    new ProcessedEventEntity(
                            payment.getEventId(),
                            LocalDateTime.now()));

            return;
        }

        DeliveryEntity delivery =
                new DeliveryEntity();

        delivery.setOrderId(
                payment.getOrderId());

        delivery.setCustomerId(
                payment.getCustomerId());

        delivery.setDeliveryAddress(
                payment.getDeliveryAddress());

        delivery.setDeliveryStatus(
                DeliveryStatus.CREATED);

        delivery.setCreatedAt(
                LocalDateTime.now());

        delivery.setUpdatedAt(
                LocalDateTime.now());

        // Temporary tracking number to obtain DB id
        delivery.setTrackingNumber(
                "TRK-TEMP-" + System.nanoTime());

        deliveryRepository.save(delivery);

        // Final tracking number
        delivery.setTrackingNumber(
                "TRK-"
                + String.format(
                        "%05d",
                        50000 + delivery.getId()));

        deliveryRepository.save(delivery);

        DeliveryCreatedEvent event =
                new DeliveryCreatedEvent();

        event.setEventId(
                idGenerator.generate());

        event.setEventType(
                "DELIVERY_CREATED");

        event.setOrderId(
                delivery.getOrderId());

        event.setCustomerId(
                delivery.getCustomerId());

        event.setTrackingNumber(
                delivery.getTrackingNumber());

        event.setDeliveryAddress(
                delivery.getDeliveryAddress());

        event.setDeliveryStatus(
                delivery.getDeliveryStatus().name());

        event.setEventTime(
                LocalDateTime.now());

        saveOutbox(
                event,
                "delivery-created",
                delivery.getOrderId());

        processedEventRepository.save(
                new ProcessedEventEntity(
                        payment.getEventId(),
                        LocalDateTime.now()));
    }

    @Transactional
    public void updateStatus(
            String orderId,
            DeliveryStatus status) {

        DeliveryEntity delivery =
                deliveryRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Delivery not found for orderId: "
                                        + orderId));

        delivery.setDeliveryStatus(status);

        delivery.setUpdatedAt(
                LocalDateTime.now());

        deliveryRepository.save(delivery);

        if (status ==
                DeliveryStatus.OUT_FOR_DELIVERY) {

            publishStatus(
                    delivery,
                    "DELIVERY_OUT_FOR_DELIVERY",
                    "delivery-out-for-delivery");
        }

        if (status ==
                DeliveryStatus.DELIVERED) {

            publishStatus(
                    delivery,
                    "ORDER_DELIVERED",
                    "order-delivered");
        }
    }

    private void publishStatus(
            DeliveryEntity delivery,
            String eventType,
            String topic) {

        try {

            DeliveryStatusEvent event =
                    new DeliveryStatusEvent();

            event.setEventId(
                    idGenerator.generate());

            event.setEventType(eventType);

            event.setOrderId(
                    delivery.getOrderId());

            event.setCustomerId(
                    delivery.getCustomerId());

            event.setTrackingNumber(
                    delivery.getTrackingNumber());

            event.setDeliveryAddress(
                    delivery.getDeliveryAddress());

            event.setDeliveryStatus(
                    delivery
                            .getDeliveryStatus()
                            .name());

            event.setEventTime(
                    LocalDateTime.now());

            saveOutbox(
                    event,
                    topic,
                    delivery.getOrderId());

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to create delivery status event",
                    ex);
        }
    }

    private void saveOutbox(
            DeliveryCreatedEvent event,
            String topic,
            String key)
            throws Exception {

        OutboxEvent outbox =
                new OutboxEvent();

        outbox.setEventId(
                event.getEventId());

        outbox.setEventType(
                event.getEventType());

        outbox.setTopic(topic);

        outbox.setMessageKey(key);

        outbox.setPayload(
                objectMapper.writeValueAsString(
                        event));

        outbox.setStatus("NEW");

        outbox.setCreatedAt(
                LocalDateTime.now());

        outboxRepository.save(outbox);
    }

    private void saveOutbox(
            DeliveryStatusEvent event,
            String topic,
            String key)
            throws Exception {

        OutboxEvent outbox =
                new OutboxEvent();

        outbox.setEventId(
                event.getEventId());

        outbox.setEventType(
                event.getEventType());

        outbox.setTopic(topic);

        outbox.setMessageKey(key);

        outbox.setPayload(
                objectMapper.writeValueAsString(
                        event));

        outbox.setStatus("NEW");

        outbox.setCreatedAt(
                LocalDateTime.now());

        outboxRepository.save(outbox);
    }
}