package com.carpass.service_api.web.dto.workshop;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.web.dto.common.AddressResponse;

/**
 * Dados da oficina.
 */
public record WorkshopResponse(UUID id, String tradeName, String corporateName, String cnpj, String phone, String email,
		String logoUrl, AddressResponse address, boolean active, boolean managerCanSeeFinance,
		Integer autoCloseOpenOrdersHours, LocalDateTime createdAt, UUID ownerId) {

	public static WorkshopResponse from(Workshop workshop) {
		return new WorkshopResponse(workshop.getId(), workshop.getTradeName(), workshop.getCorporateName(),
				workshop.getCnpj(), workshop.getPhone(), workshop.getEmail(), workshop.getLogoUrl(),
				AddressResponse.from(workshop.getAddress()), workshop.isActive(), workshop.isManagerCanSeeFinance(),
				workshop.getAutoCloseOpenOrdersHours(), workshop.getCreatedAt(),
				workshop.getOwner() == null ? null : workshop.getOwner().getId());
	}
}
