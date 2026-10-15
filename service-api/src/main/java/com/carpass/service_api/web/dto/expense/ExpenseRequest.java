package com.carpass.service_api.web.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de despesa.
 *
 * <p>
 * O valor tem piso maior que zero, nao "nao negativo": despesa de R$ 0,00 nao
 * existe no mundo real e so serviria para sujar a contagem de lancamentos. O
 * limite de duas casas evita que um centavo se perca no arredondamento entre a
 * tela e o banco.
 * </p>
 *
 * <p>
 * A oficina <strong>nao</strong> vem daqui, e o responsavel tambem nao: os dois
 * saem do usuario autenticado. Aceitar qualquer um dos dois no corpo seria
 * entregar a chave do isolamento entre oficinas junto com o formulario.
 * </p>
 */
public record ExpenseRequest(

		@NotBlank(message = "Informe a descricao da despesa")
		@Size(max = 180)
		String description,

		@NotNull(message = "Informe o valor da despesa")
		@DecimalMin(value = "0.01", message = "O valor da despesa precisa ser maior que zero")
		@Digits(integer = 10, fraction = 2, message = "Valor invalido")
		BigDecimal amount,

		@NotNull(message = "Informe a data da despesa")
		LocalDate expenseDate,

		@NotNull(message = "Escolha a categoria da despesa")
		UUID categoryId,

		@Size(max = 1000)
		String notes) {
}
