package com.carpass.service_api.web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualizacao dos dados do proprio usuario. O papel e a oficina nao entram aqui
 * de proposito: elevar o proprio papel seria uma escalacao de privilegio.
 */
public record UpdateProfileRequest(

		@NotBlank(message = "Informe o nome")
		@Size(max = 80)
		String firstName,

		@Size(max = 80)
		String lastName,

		@NotBlank(message = "Informe o e-mail")
		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email,

		@Size(max = 30)
		String phone) {
}
