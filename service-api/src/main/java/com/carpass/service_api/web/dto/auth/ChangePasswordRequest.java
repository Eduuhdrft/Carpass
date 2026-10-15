package com.carpass.service_api.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha feita pelo proprio usuario ja autenticado. Exige a senha atual
 * para impedir que uma sessao sequestrada altere a credencial.
 */
public record ChangePasswordRequest(

		@NotBlank(message = "Informe a senha atual")
		String currentPassword,

		@NotBlank(message = "Informe a nova senha")
		@Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve conter ao menos uma letra e um numero")
		String newPassword) {
}
