package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.WorkOrder;

/**
 * Versao enxuta da ordem de servico, para listagens e para o painel.
 *
 * <p>
 * Carrega ja resolvidos o nome do cliente e a placa, que e o que a tela mostra,
 * evitando uma chamada por linha da lista.
 * </p>
 */
public record WorkOrderSummaryResponse(UUID id, Integer number, WorkOrderStatus status, String statusLabel,
		QuoteStatus quoteStatus, String quoteStatusLabel, UUID customerId, String customerName, UUID vehicleId,
		String vehiclePlate, String vehicleDescription, String assignedMechanicName, BigDecimal totalAmount,
		LocalDateTime openedAt, LocalDate estimatedDeliveryDate, LocalDateTime deliveredAt) {

	public static WorkOrderSummaryResponse from(WorkOrder order) {
		return new WorkOrderSummaryResponse(order.getId(), order.getNumber(), order.getStatus(),
				order.getStatus().getLabel(), order.getQuoteStatus(), order.getQuoteStatus().getLabel(),
				order.getCustomer().getId(), order.getCustomer().getName(), order.getVehicle().getId(),
				order.getVehicle().getPlate(), order.getVehicle().getBrand() + " " + order.getVehicle().getModel(),
				order.getAssignedMechanic() == null ? null : order.getAssignedMechanic().getName(),
				order.getTotalAmount(), order.getOpenedAt(), order.getEstimatedDeliveryDate(), order.getDeliveredAt());
	}
}
