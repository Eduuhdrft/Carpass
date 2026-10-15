package com.carpass.service_api.web.dto.parttype;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.PartType;

/**
 * Tipo de peca devolvido pela API.
 *
 * @param partCount quantas pecas ativas usam este tipo. Vai junto para a tela
 *                  poder avisar o que se perde de vista ao desativar um tipo --
 *                  desativar "Vela" com 30 velas no estoque nao pode ser um
 *                  clique sem consequencia aparente.
 */
public record PartTypeResponse(UUID id, String name, String description, long partCount, boolean active,
		LocalDateTime createdAt) {

	public static PartTypeResponse from(PartType type) {
		return from(type, 0L);
	}

	public static PartTypeResponse from(PartType type, long partCount) {
		return new PartTypeResponse(type.getId(), type.getName(), type.getDescription(), partCount, type.isActive(),
				type.getCreatedAt());
	}
}
