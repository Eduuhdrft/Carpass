package com.carpass.service_api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.PartStockMovement;

/**
 * Historico de movimentacao de estoque.
 */
public interface PartStockMovementRepository extends JpaRepository<PartStockMovement, UUID> {

	Page<PartStockMovement> findByWorkshopIdAndPartIdOrderByCreatedAtDesc(UUID workshopId, UUID partId,
			Pageable pageable);
}
