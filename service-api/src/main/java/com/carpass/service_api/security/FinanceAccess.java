package com.carpass.service_api.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.ForbiddenOperationException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.WorkshopRepository;

/**
 * Quem pode ver e quem pode mexer no financeiro.
 *
 * <h2>Por que isto e uma classe, e nao uma anotacao</h2>
 *
 * <p>
 * A regra tem duas partes: o dono sempre ve, e o gerente ve <em>se o dono
 * deixar</em>. A segunda depende de uma coluna da oficina, e
 * {@code @PreAuthorize} nao alcanca o banco sem virar SpEL ilegivel. Ficando
 * aqui, a regra existe em um lugar so -- e regra de permissao duplicada e
 * exatamente onde nasce a brecha: alguem corrige uma copia e esquece a outra.
 * </p>
 *
 * <h2>Ver e lancar sao coisas diferentes</h2>
 *
 * <p>
 * Lancar despesa altera o resultado da oficina, entao continua so com o dono e o
 * gerente que ele autorizou -- a mesma porta, nao uma porta a mais. Atendente e
 * mecanico nao passam por nenhuma das duas.
 * </p>
 */
@Component
public class FinanceAccess {

	private final CurrentUser currentUser;
	private final WorkshopRepository workshopRepository;

	public FinanceAccess(CurrentUser currentUser, WorkshopRepository workshopRepository) {
		this.currentUser = currentUser;
		this.workshopRepository = workshopRepository;
	}

	/**
	 * Se o usuario da requisicao pode ver numeros financeiros.
	 *
	 * <p>
	 * Usado tambem pelo painel, que devolve o faturamento nulo em vez de negar a
	 * tela inteira: ali o numero e um cartao entre outros, e o resto da tela serve
	 * a todo mundo.
	 * </p>
	 */
	public boolean canView() {
		if (currentUser.hasAnyRole(Role.OWNER)) {
			return true;
		}
		if (!currentUser.hasAnyRole(Role.MANAGER)) {
			return false;
		}
		return loadWorkshop().isManagerCanSeeFinance();
	}

	/** Barra a requisicao inteira quando a tela e financeira de ponta a ponta. */
	public void requireView() {
		if (!canView()) {
			throw new ForbiddenOperationException(
					"Voce nao tem permissao para ver o financeiro desta oficina.");
		}
	}

	/**
	 * Barra quem tenta cadastrar, editar ou excluir despesa.
	 *
	 * <p>
	 * Hoje coincide com {@link #canView()}, e continua sendo um metodo proprio: no
	 * dia em que ler e escrever se separarem, o lugar de mudar ja esta escolhido e
	 * as chamadas ja estao no lugar certo.
	 * </p>
	 */
	public void requireManage() {
		if (!canView()) {
			throw new ForbiddenOperationException(
					"Voce nao tem permissao para lancar despesas nesta oficina.");
		}
	}

	/** Somente o dono decide se o gerente ve o financeiro. */
	public void requireOwner() {
		if (!currentUser.hasAnyRole(Role.OWNER)) {
			throw new ForbiddenOperationException(
					"Somente o proprietario da oficina pode alterar quem ve o financeiro.");
		}
	}

	private Workshop loadWorkshop() {
		UUID workshopId = currentUser.requireWorkshopId();
		return workshopRepository.findById(workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Oficina"));
	}
}
