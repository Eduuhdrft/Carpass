package com.carpass.service_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.WorkOrderItem;

/**
 * Itens de ordens de servico.
 */
public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, UUID> {
}
