package com.carpass.service_api.web.dto.user;

import java.util.UUID;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Dados do usuario expostos pela API. Nunca inclui a senha.
 *
 * <p>
 * Papel e oficina vem do <strong>vinculo da sessao</strong>, nao da pessoa:
 * quem tem vinculo em duas oficinas tem um papel em cada uma, e o que vale aqui
 * e o da oficina em que a sessao esta. Os tres campos vem nulos quando nao ha
 * vinculo -- estado legitimo de quem foi demitido e ainda nao criou a propria
 * oficina.
 * </p>
 */
public record UserResponse(UUID id, String firstName, String lastName, String fullName, String email, String phone,
		Role role, String roleLabel, boolean active, UUID workshopId, String workshopName) {

	/** Sem vinculo: a pessoa esta autenticada, mas fora de qualquer oficina. */
	public static UserResponse from(User user) {
		return from(user, null);
	}

	public static UserResponse from(User user, WorkshopMember membership) {
		Role role = membership == null ? null : membership.getRole();
		return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getFullName(),
				user.getEmail(), user.getPhone(), role, role == null ? null : role.getLabel(), user.isActive(),
				membership == null ? null : membership.getWorkshop().getId(),
				membership == null ? null : membership.getWorkshop().getTradeName());
	}
}
