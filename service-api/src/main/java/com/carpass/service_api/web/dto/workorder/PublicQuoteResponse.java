package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.model.QuoteApprovalToken;
import com.carpass.service_api.domain.model.WorkOrder;

/**
 * Orcamento como o cliente ve, fora do sistema.
 *
 * <p>
 * Este DTO e deliberadamente enxuto. Ele atende uma rota publica, entao carrega
 * apenas o que o cliente precisa para decidir: a identificacao do carro, o que
 * sera feito e quanto custa. Nada de id interno, observacao da equipe,
 * diagnostico tecnico ou dado de outro cliente.
 * </p>
 */
public record PublicQuoteResponse(String workshopName, String workshopPhone, Integer orderNumber, String customerName,
		String vehiclePlate, String vehicleDescription, String reportedProblem, List<Item> items,
		BigDecimal servicesTotal, BigDecimal partsTotal, BigDecimal discount, BigDecimal totalAmount,
		LocalDate estimatedDeliveryDate, String status, boolean pending) {

	/** Uma linha do orcamento. */
	public record Item(String description, String typeLabel, Integer quantity, BigDecimal unitPrice,
			BigDecimal totalPrice) {
	}

	public static PublicQuoteResponse from(QuoteApprovalToken token) {
		WorkOrder order = token.getWorkOrder();

		List<Item> items = order.getItems().stream()
				.map(item -> new Item(item.getDescription(), item.getType().getLabel(), item.getQuantity(),
						item.getUnitPrice(), item.getTotalPrice()))
				.toList();

		// "Pendente" e o que decide se a tela mostra os botoes ou apenas o aviso de
		// que a proposta ja foi respondida.
		boolean pending = order.getQuoteStatus() == QuoteStatus.PENDING;

		return new PublicQuoteResponse(
				order.getWorkshop().getTradeName(),
				order.getWorkshop().getPhone(),
				order.getNumber(),
				order.getCustomer().getName(),
				order.getVehicle().getPlate(),
				vehicleDescription(order),
				order.getReportedProblem(),
				items,
				order.getServicesTotal(),
				order.getPartsTotal(),
				order.getDiscount(),
				order.getTotalAmount(),
				order.getEstimatedDeliveryDate(),
				order.getQuoteStatus().getLabel(),
				pending);
	}

	private static String vehicleDescription(WorkOrder order) {
		StringBuilder description = new StringBuilder();
		description.append(order.getVehicle().getBrand()).append(' ').append(order.getVehicle().getModel());
		if (order.getVehicle().getModelYear() != null) {
			description.append(" · ").append(order.getVehicle().getModelYear());
		}
		return description.toString();
	}
}
