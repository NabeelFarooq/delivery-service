package com.ecommerce.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.delivery.entity.ProcessedEventEntity;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEventEntity, String> {
}