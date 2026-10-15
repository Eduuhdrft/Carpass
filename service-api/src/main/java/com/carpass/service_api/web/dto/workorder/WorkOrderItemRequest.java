package com.carpass.service_api.web.dto.workorder;

import java.math.BigDecimal;
import java.util.UUID;

import com.carpass.service_api.domain.enums.ItemType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Servico executado ou peca aplicada em uma ordem de servico.
 *
 * <p>
 * Quando {@code partId} ou {@code serviceCatalogId} vem preenchido, descricao e
 * preco sao herdados do cadastro caso nao sejam informados. Isso permite tanto
 * lancar um item do catalogo quanto digitar um item avulso.
 * </p>
 */
public record WorkOrderItemRequest(

		@NotNull(message = "Informe o tipo do item")
		ItemType type,

		/** Peca do estoque. Obrigatorio quando o tipo for PART e o item nao for avulso. */
		UUID partId,

		/** Servico do catalogo. Opcional: o item pode ser digitado livremente. */
		UUID serviceCatalogId,

		@Size(max = 300, message = "Descricao muito longa")
		String description,

		@NotNull(message = "Informe a quantidade")
		@Positive(message = "A quantidade deve ser maior que zero")
		Integer quantity,

		@DecimalMin(value = "0.00", message = "O valor unitario nao pode ser negativo")
		@Digits(integer = 10, fraction = 2, message = "Valor unitario invalido")
		BigDecimal unitPrice) {
}
