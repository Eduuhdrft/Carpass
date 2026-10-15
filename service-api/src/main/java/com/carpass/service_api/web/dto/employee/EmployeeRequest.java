package com.carpass.service_api.web.dto.employee;

import java.util.UUID;

import com.carpass.service_api.domain.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de funcionario.
 *
 * <p>
 * {@code userId} e opcional e serve para amarrar o funcionario a uma conta de
 * acesso ja existente na mesma oficina. Nem todo funcionario precisa de login.
 * </p>
 */
public record EmployeeRequest(

		@NotBlank(message = "Informe o nome")
		@Size(max = 150)
		String name,

		@NotNull(message = "Informe o cargo")
		Role role,

		@Size(max = 120)
		String specialty,

		@Size(max = 30)
		String phone,

		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email,

		UUID userId) {
}
