package com.carpass.service_api.dto.manutencaoDto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManutencaoCreateDto(
		@NotNull(message = "Precisa estar associado a um veículo")
		UUID veiculo_id,
		@NotBlank(message = "tipo serviço não pode ser nulo")
		String tipoServico,
		@NotNull(message = "km momento não pode ser nulo")
		Integer kmMomento,
		BigDecimal valor,
		@NotBlank(message = "descrição manutenção não pode ser nulo")
		String descricaoManutencao,
		@NotBlank(message = "nome oficina não pode ser nulo")
		String nomeOficina
	) {}
