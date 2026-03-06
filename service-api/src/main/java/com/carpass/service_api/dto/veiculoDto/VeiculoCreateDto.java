package com.carpass.service_api.dto.veiculoDto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VeiculoCreateDto(
		@NotBlank(message = "Placa é obrigatório")
		String placa,
		@NotBlank(message = "Marca é obrigatório")
		String marca,
		@NotBlank(message = "Modelo é obrigatório")
		String modelo,
		Integer anoDeFabricacao,
		String cor,
		@NotNull(message = "Usuario é obrigatório")
		UUID usuario_id
		) {}
