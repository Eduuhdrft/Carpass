package com.carpass.service_api.web.dto.customer;

import com.carpass.service_api.domain.enums.CustomerType;
import com.carpass.service_api.web.dto.common.AddressRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de cliente.
 *
 * <p>
 * A oficina nao entra aqui de proposito: ela vem sempre do usuario autenticado.
 * </p>
 */
public record CustomerRequest(

		@NotNull(message = "Informe o tipo de cliente")
		CustomerType type,

		@NotBlank(message = "Informe o nome")
		@Size(max = 180, message = "Nome muito longo")
		String name,

		@Pattern(regexp = "^$|\\d{11}|\\d{14}", message = "Informe 11 digitos para CPF ou 14 para CNPJ, sem pontuacao")
		String document,

		@Size(max = 30, message = "Telefone muito longo")
		String phone,

		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email,

		@Valid
		AddressRequest address,

		@Size(max = 1000, message = "Observacoes muito longas")
		String notes) {
}
