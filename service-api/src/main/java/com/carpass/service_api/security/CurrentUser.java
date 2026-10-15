package com.carpass.service_api.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.exception.ForbiddenOperationException;

/**
 * Ponto unico de acesso ao usuario autenticado e a oficina em que ele opera.
 *
 * <p>
 * Todo servico que manipula dados operacionais deve obter o
 * {@link #requireWorkshopId()} daqui, e nunca aceitar o identificador da oficina
 * vindo do corpo da requisicao. E isso que garante o isolamento entre oficinas.
 * </p>
 */
@Component
public class CurrentUser {

	/** Principal autenticado, se houver. */
	public Optional<AuthenticatedUser> find() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
			return Optional.empty();
		}
		return Optional.of(principal);
	}

	/** Principal autenticado. Lanca 403 quando nao ha usuario no contexto. */
	public AuthenticatedUser require() {
		return find().orElseThrow(() -> new ForbiddenOperationException("Nenhum usuario autenticado na requisicao."));
	}

	/** Entidade do usuario autenticado. */
	public User requireUser() {
		return require().getUser();
	}

	public UUID requireId() {
		return require().getId();
	}

	/**
	 * Oficina em que a requisicao opera.
	 *
	 * <p>
	 * Vem do vinculo que o filtro conferiu contra o banco nesta requisicao, e
	 * nunca do corpo do pedido. E isso que garante o isolamento entre oficinas.
	 * </p>
	 *
	 * @throws ForbiddenOperationException quando nao ha vinculo escolhido -- o
	 *                                     caso de quem foi demitido e ainda nao
	 *                                     criou a propria oficina, e o do
	 *                                     administrador da plataforma
	 */
	public UUID requireWorkshopId() {
		UUID workshopId = require().getWorkshopId();
		if (workshopId == null) {
			throw new ForbiddenOperationException(
					"Voce nao esta vinculado a nenhuma oficina. Crie a sua oficina para comecar.");
		}
		return workshopId;
	}

	/** Se ha uma oficina escolhida nesta requisicao. */
	public boolean hasWorkshop() {
		return find().map(principal -> principal.getWorkshopId() != null).orElse(false);
	}

	public boolean isSystemAdmin() {
		return find().map(principal -> principal.getUser().isPlatformAdmin()).orElse(false);
	}

	/**
	 * Se o papel na oficina desta requisicao esta entre os informados.
	 *
	 * <p>
	 * Papel nulo -- ou seja, sem vinculo -- nao casa com nada, que e o padrao
	 * seguro: quem nao pertence a oficina nenhuma nao passa por checagem alguma
	 * de papel.
	 * </p>
	 */
	public boolean hasAnyRole(Role... roles) {
		return find().map(principal -> {
			Role atual = principal.getRole();
			if (atual == null) {
				return false;
			}
			for (Role role : roles) {
				if (atual == role) {
					return true;
				}
			}
			return false;
		}).orElse(false);
	}
}
