package com.ecommerce.delivery.util;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventIdGenerator {

    private static final long EPOCH =
            1704067200000L;

    private final long workerId;

    private long lastTimestamp = -1L;

    private long sequence = 0L;

    public DeliveryEventIdGenerator(
            @Value("${event.worker-id:1}")
            long workerId) {

        if (workerId < 0 || workerId > 1023) {
            throw new IllegalArgumentException(
                    "event.worker-id out of range");
        }

        this.workerId = workerId;
    }

    public synchronized String generate() {

        long timestamp =
                Instant.now().toEpochMilli();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "System clock moved backwards");
        }

        if (timestamp == lastTimestamp) {

            sequence =
                    (sequence + 1) & 4095;

            if (sequence == 0) {
                timestamp =
                        next(timestamp);
            }

        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        long number =
                ((timestamp - EPOCH) << 22)
                | (workerId << 12)
                | sequence;

        return "DEL-" + number;
    }

    private long next(long timestamp) {

        long current =
                Instant.now().toEpochMilli();

        while (current <= timestamp) {
            current =
                    Instant.now().toEpochMilli();
        }

        return current;
    }
}