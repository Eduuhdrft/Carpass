package com.carpass.service_api.web.dto.servicecatalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de um servico do catalogo.
 */
public record ServiceCatalogRequest(

		@NotBlank(message = "Informe o nome do servico")
		@Size(max = 150)
		String name,

		@Size(max = 1000)
		String description,

		@DecimalMin(value = "0.00", message = "O preco nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Preco invalido")
		BigDecimal defaultPrice,

		@Positive(message = "O tempo estimado deve ser maior que zero")
		Integer estimatedMinutes) {
}
