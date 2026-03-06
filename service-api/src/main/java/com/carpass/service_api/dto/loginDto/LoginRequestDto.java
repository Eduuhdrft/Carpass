package com.carpass.service_api.dto.loginDto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
		@NotBlank(message = "Email é obrigatório")
		String email,
		@NotBlank(message = "Senha é obrigatório")
		String senha
	) {}
