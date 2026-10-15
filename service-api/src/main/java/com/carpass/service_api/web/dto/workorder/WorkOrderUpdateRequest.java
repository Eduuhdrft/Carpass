package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Edicao dos dados da ordem de servico.
 *
 * <p>
 * Nao inclui status nem itens: os dois tem endpoints proprios porque carregam
 * regras que uma edicao livre nao pode furar. O status obedece a maquina de
 * estados e os itens movimentam estoque.
 * </p>
 */
public record WorkOrderUpdateRequest(

		UUID assignedMechanicId,

		@Size(max = 2000)
		String reportedProblem,

		@Size(max = 2000)
		String diagnosis,

		@PositiveOrZero(message = "A quilometragem nao pode ser negativa")
		Integer mileage,

		LocalDate estimatedDeliveryDate,

		@DecimalMin(value = "0.00", message = "O desconto nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Desconto invalido")
		BigDecimal discount,

		@Size(max = 2000)
		String notes) {
}
