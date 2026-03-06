package com.carpass.service_api.dto.usuarioDto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record SenhaUpdateDto(
				UUID id,
				@NotBlank(message = "Senha é obrigatório!")
				String senha
			) {}
