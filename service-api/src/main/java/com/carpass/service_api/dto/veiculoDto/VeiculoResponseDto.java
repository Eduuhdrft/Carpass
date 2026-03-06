package com.carpass.service_api.dto.veiculoDto;

import java.util.UUID;

public record VeiculoResponseDto(
		UUID id,
		String placa,
		String marca,
		String modelo,
		Integer anoDeFabricacao,
		String cor,
		UUID usuario_id
		) {}
