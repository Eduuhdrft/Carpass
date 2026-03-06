package com.carpass.service_api.dto.usuarioDto;

import java.util.UUID;

import com.carpass.service_api.enums.UsuarioRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioUpdateDto(
		UUID id,
		@NotBlank(message = "Nome é obrigatório")
		String nome,
		String sobrenome,
		@NotBlank(message = "Email é obrigatório")
		@Email(message = "Email inválido")
		String email,
		String numero,
		@NotNull(message = "Tipo conta é obrigatório")
		UsuarioRole tipoConta
		) {}
