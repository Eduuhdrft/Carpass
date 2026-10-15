package com.carpass.service_api.web.dto.part;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartType;

/**
 * Peca devolvida pela API.
 *
 * @param partTypeId        id do tipo, para a tela preencher o seletor
 * @param partTypeName      nome do tipo, para a listagem nao precisar de uma
 *                          segunda busca so para escrever "Oleo de motor"
 * @param belowMinimumStock calculado no servidor para a listagem ja poder
 *                          destacar o que precisa de reposicao
 */
public record PartResponse(UUID id, String code, String name, String manufacturer, UUID partTypeId,
		String partTypeName, BigDecimal costPrice, BigDecimal salePrice, Integer stockQuantity, Integer minimumStock,
		boolean belowMinimumStock, boolean active, LocalDateTime createdAt) {

	public static PartResponse from(Part part) {
		PartType type = part.getPartType();
		return new PartResponse(part.getId(), part.getCode(), part.getName(), part.getManufacturer(),
				type == null ? null : type.getId(), type == null ? null : type.getName(),
				part.getCostPrice(), part.getSalePrice(), part.getStockQuantity(), part.getMinimumStock(),
				part.isBelowMinimumStock(), part.isActive(), part.getCreatedAt());
	}
}
