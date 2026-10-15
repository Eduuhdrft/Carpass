package com.carpass.service_api.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Redefinicao de senha usando o token recebido por e-mail.
 */
public record ResetPasswordRequest(

		@NotBlank(message = "Token obrigatorio")
		String token,

		@NotBlank(message = "Informe a nova senha")
		@Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve conter ao menos uma letra e um numero")
		String newPassword) {
}
