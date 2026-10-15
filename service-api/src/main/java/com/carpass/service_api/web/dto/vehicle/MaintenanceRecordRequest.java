package com.carpass.service_api.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Lancamento manual no historico do veiculo.
 *
 * <p>
 * Serve para registrar servicos feitos fora desta oficina ou para importar o
 * historico anterior de um veiculo que acabou de chegar. O que a propria oficina
 * executa entra no historico automaticamente pelas ordens de servico.
 * </p>
 */
public record MaintenanceRecordRequest(

		@NotNull(message = "Informe a data do servico")
		@PastOrPresent(message = "A data do servico nao pode estar no futuro")
		LocalDate serviceDate,

		@PositiveOrZero(message = "A quilometragem nao pode ser negativa")
		Integer mileage,

		@NotBlank(message = "Descreva o servico realizado")
		@Size(max = 500)
		String description,

		@Size(max = 150)
		String performedBy,

		@DecimalMin(value = "0.00", message = "O valor nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Valor invalido")
		BigDecimal amount,

		@Size(max = 1000)
		String notes) {
}
