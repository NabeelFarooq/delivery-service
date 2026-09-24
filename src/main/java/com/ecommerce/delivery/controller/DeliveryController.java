package com.ecommerce.delivery.controller;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.ecommerce.delivery.entity.DeliveryStatus;

import com.ecommerce.delivery.service.DeliveryProcessor;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryProcessor processor;

    public DeliveryController(
            DeliveryProcessor processor) {

        this.processor = processor;
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable String orderId,
            @RequestParam DeliveryStatus status) {

        processor.updateStatus(
                orderId,
                status);

        return ResponseEntity.ok(
                "Delivery status updated to "
                + status);
    }
}