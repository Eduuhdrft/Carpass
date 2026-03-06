package com.carpass.service_api.dto.manutencaoDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManutencaoResponseDto(
		UUID id,
		String placa,
		String tipoServico,
		LocalDate dataManutencao,
		Integer kmMomento,
		BigDecimal valor,
		String descricaoManutencao,
		String nomeOficina
	) {}
