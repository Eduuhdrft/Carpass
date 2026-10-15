package com.carpass.service_api.web.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de cadastro e edicao de categoria de despesa. */
public record ExpenseCategoryRequest(

		@NotBlank(message = "Informe o nome da categoria")
		@Size(max = 80)
		String name,

		@Size(max = 300)
		String description) {
}
