package com.carpass.service_api.web.dto.part;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de peca.
 *
 * <p>
 * A quantidade em estoque nao entra aqui: ela so muda por movimentacao
 * registrada, para que todo saldo tenha rastreabilidade. No cadastro inicial
 * usamos {@code initialStock}, que gera a primeira movimentacao de entrada.
 * </p>
 */
public record PartRequest(

		@Size(max = 60)
		String code,

		@NotBlank(message = "Informe o nome da peca")
		@Size(max = 150)
		String name,

		@Size(max = 120)
		String manufacturer,

		/*
		 * Opcional de proposito: peca lancada as pressas no balcao entra sem tipo
		 * e e classificada depois. Obrigar faria alguem escolher qualquer um so
		 * para conseguir salvar, e tipo errado e pior que tipo nenhum.
		 */
		UUID partTypeId,

		@DecimalMin(value = "0.00", message = "O preco de custo nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Preco de custo invalido")
		BigDecimal costPrice,

		@DecimalMin(value = "0.00", message = "O preco de venda nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Preco de venda invalido")
		BigDecimal salePrice,

		@NotNull(message = "Informe o estoque minimo")
		@PositiveOrZero(message = "O estoque minimo nao pode ser negativo")
		Integer minimumStock,

		@PositiveOrZero(message = "O estoque inicial nao pode ser negativo")
		Integer initialStock) {
}
