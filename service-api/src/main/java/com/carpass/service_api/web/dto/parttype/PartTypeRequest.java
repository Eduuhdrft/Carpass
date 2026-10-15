package com.carpass.service_api.web.dto.parttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de tipo de peca.
 */
public record PartTypeRequest(

		@NotBlank(message = "Informe o nome do tipo")
		@Size(max = 80)
		String name,

		@Size(max = 300)
		String description) {
}
