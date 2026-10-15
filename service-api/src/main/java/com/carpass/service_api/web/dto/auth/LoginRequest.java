package com.carpass.service_api.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais enviadas na tela de login.
 */
public record LoginRequest(

		@NotBlank(message = "Informe o e-mail")
		@Email(message = "E-mail invalido")
		String email,

		@NotBlank(message = "Informe a senha")
		String password) {
}
