package com.carpass.service_api.web.dto.auth;

import java.util.UUID;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Uma oficina em que a pessoa pode entrar, com o papel que ela tem la.
 *
 * <p>
 * Alimenta tanto a escolha no login quanto o seletor de troca de oficina. O
 * papel aparece junto porque a mesma pessoa pode ser dona de uma e mecanica em
 * outra -- so o nome da oficina nao diria em que pe ela entra.
 * </p>
 */
public record WorkshopOption(UUID workshopId, String workshopName, Role role, String roleLabel) {

	public static WorkshopOption from(WorkshopMember membership) {
		return new WorkshopOption(membership.getWorkshop().getId(), membership.getWorkshop().getTradeName(),
				membership.getRole(), membership.getRole().getLabel());
	}
}
