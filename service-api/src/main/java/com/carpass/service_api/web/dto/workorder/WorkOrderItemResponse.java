package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.util.UUID;

import com.carpass.service_api.domain.enums.ItemType;
import com.carpass.service_api.domain.model.WorkOrderItem;

/**
 * Item de uma ordem de servico.
 */
public record WorkOrderItemResponse(UUID id, ItemType type, String typeLabel, UUID partId, UUID serviceCatalogId,
		String description, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {

	public static WorkOrderItemResponse from(WorkOrderItem item) {
		return new WorkOrderItemResponse(item.getId(), item.getType(), item.getType().getLabel(),
				item.getPart() == null ? null : item.getPart().getId(),
				item.getServiceCatalog() == null ? null : item.getServiceCatalog().getId(), item.getDescription(),
				item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
	}
}
