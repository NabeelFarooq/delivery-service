package com.ecommerce.delivery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.delivery.entity.DeliveryEntity;

public interface DeliveryRepository
        extends JpaRepository<DeliveryEntity, Long> {

    Optional<DeliveryEntity> findByOrderId(String orderId);
}