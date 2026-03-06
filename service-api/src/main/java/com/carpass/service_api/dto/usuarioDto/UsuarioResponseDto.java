package com.carpass.service_api.dto.usuarioDto;

import java.util.UUID;

import com.carpass.service_api.enums.UsuarioRole;

public record UsuarioResponseDto(
		UUID id,
		String nome,
		String sobrenome,
		String email,
		String numero,
		UsuarioRole tipoConta
		) {}
