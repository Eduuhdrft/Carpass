package com.carpass.service_api.web.dto.part;

import com.carpass.service_api.domain.enums.StockMovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Entrada, saida ou ajuste manual de estoque.
 *
 * <p>
 * Para {@code INBOUND} e {@code OUTBOUND}, a quantidade e somada ou subtraida do
 * saldo. Para {@code ADJUSTMENT}, a quantidade e o novo saldo absoluto, usado
 * quando a contagem fisica diverge do sistema.
 * </p>
 */
public record StockMovementRequest(

		@NotNull(message = "Informe o tipo de movimentacao")
		StockMovementType type,

		@NotNull(message = "Informe a quantidade")
		@Positive(message = "A quantidade deve ser maior que zero")
		Integer quantity,

		@Size(max = 300)
		String reason) {
}
