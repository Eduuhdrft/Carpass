package com.carpass.service_api.web.dto.part;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.model.PartStockMovement;

/**
 * Movimentacao de estoque devolvida pela API.
 */
public record StockMovementResponse(UUID id, StockMovementType type, String typeLabel, Integer quantity,
		Integer previousStock, Integer resultingStock, String reason, UUID workOrderId, String createdBy,
		LocalDateTime createdAt) {

	public static StockMovementResponse from(PartStockMovement movement) {
		return new StockMovementResponse(movement.getId(), movement.getType(), movement.getType().getLabel(),
				movement.getQuantity(), movement.getPreviousStock(), movement.getResultingStock(), movement.getReason(),
				movement.getWorkOrder() == null ? null : movement.getWorkOrder().getId(),
				movement.getCreatedBy() == null ? null : movement.getCreatedBy().getFullName(),
				movement.getCreatedAt());
	}
}
