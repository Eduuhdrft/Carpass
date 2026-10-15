package com.carpass.service_api.web.dto.workorder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Abertura de uma ordem de servico.
 *
 * <p>
 * O veiculo define o cliente: nao aceitamos os dois separadamente para nao abrir
 * espaco para uma OS com veiculo de um cliente e cobranca em outro.
 * </p>
 */
public record WorkOrderCreateRequest(

		@NotNull(message = "Informe o veiculo")
		UUID vehicleId,

		UUID assignedMechanicId,

		@Size(max = 2000)
		String reportedProblem,

		@PositiveOrZero(message = "A quilometragem nao pode ser negativa")
		Integer mileage,

		LocalDate estimatedDeliveryDate,

		@Size(max = 2000)
		String notes,

		@Valid
		List<WorkOrderItemRequest> items) {
}
