package com.carpass.service_api.web.dto.servicecatalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.ServiceCatalog;

/**
 * Servico do catalogo devolvido pela API.
 */
public record ServiceCatalogResponse(UUID id, String name, String description, BigDecimal defaultPrice,
		Integer estimatedMinutes, boolean active, LocalDateTime createdAt) {

	public static ServiceCatalogResponse from(ServiceCatalog service) {
		return new ServiceCatalogResponse(service.getId(), service.getName(), service.getDescription(),
				service.getDefaultPrice(), service.getEstimatedMinutes(), service.isActive(), service.getCreatedAt());
	}
}
