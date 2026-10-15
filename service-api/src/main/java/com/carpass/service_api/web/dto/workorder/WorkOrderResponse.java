package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.WorkOrder;

/**
 * Ordem de servico completa.
 *
 * @param allowedNextStatuses estados para os quais esta OS pode ir agora. A
 *                            interface usa isso para so oferecer acoes validas,
 *                            em vez de descobrir o erro depois do clique.
 * @param editable            false quando a OS ja esta finalizada, entregue ou
 *                            cancelada, ou quando o orcamento aprovado tranca os
 *                            itens
 */
public record WorkOrderResponse(UUID id, Integer number, WorkOrderStatus status, String statusLabel,
		Set<WorkOrderStatus> allowedNextStatuses, boolean editable, QuoteStatus quoteStatus, String quoteStatusLabel,
		String quoteRejectionReason,
		UUID customerId, String customerName, UUID vehicleId, String vehiclePlate, String vehicleDescription,
		UUID assignedMechanicId, String assignedMechanicName, String reportedProblem, String diagnosis, String notes,
		Integer mileage, List<WorkOrderItemResponse> items, BigDecimal partsTotal, BigDecimal servicesTotal,
		BigDecimal discount, BigDecimal totalAmount, LocalDate estimatedDeliveryDate, LocalDateTime openedAt,
		LocalDateTime approvedAt, LocalDateTime finishedAt, LocalDateTime deliveredAt, LocalDateTime canceledAt) {

	public static WorkOrderResponse from(WorkOrder order, boolean editable) {
		return new WorkOrderResponse(order.getId(), order.getNumber(), order.getStatus(), order.getStatus().getLabel(),
				order.getStatus().allowedNextStatuses(), editable, order.getQuoteStatus(),
				order.getQuoteStatus().getLabel(), order.getQuoteRejectionReason(),
				order.getCustomer().getId(), order.getCustomer().getName(),
				order.getVehicle().getId(), order.getVehicle().getPlate(),
				order.getVehicle().getBrand() + " " + order.getVehicle().getModel(),
				order.getAssignedMechanic() == null ? null : order.getAssignedMechanic().getId(),
				order.getAssignedMechanic() == null ? null : order.getAssignedMechanic().getName(),
				order.getReportedProblem(), order.getDiagnosis(), order.getNotes(), order.getMileage(),
				order.getItems().stream().map(WorkOrderItemResponse::from).toList(), order.getPartsTotal(),
				order.getServicesTotal(), order.getDiscount(), order.getTotalAmount(),
				order.getEstimatedDeliveryDate(), order.getOpenedAt(), order.getApprovedAt(), order.getFinishedAt(),
				order.getDeliveredAt(), order.getCanceledAt());
	}
}
