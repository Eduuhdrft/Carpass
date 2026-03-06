package com.carpass.service_api.dto.veiculoDto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record VeiculoUpdateDto(
		UUID id,
		@NotBlank(message = "Placa é obrigatório")
		String placa,
		@NotBlank(message = "Marca é obrigatório")
		String marca,
		@NotBlank(message = "Modelo é obrigatório")
		String modelo,
		Integer anoDeFabricacao,
		String cor,
		UUID usuario_id
		) {
}
