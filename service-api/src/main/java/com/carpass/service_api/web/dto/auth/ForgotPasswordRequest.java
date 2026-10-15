package com.carpass.service_api.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de link de redefinicao de senha.
 */
public record ForgotPasswordRequest(

		@NotBlank(message = "Informe o e-mail")
		@Email(message = "E-mail invalido")
		String email) {
}
