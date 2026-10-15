package com.carpass.service_api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.WorkOrderStatusHistory;

/**
 * Linha do tempo de status das ordens de servico.
 */
public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, UUID> {

	List<WorkOrderStatusHistory> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
