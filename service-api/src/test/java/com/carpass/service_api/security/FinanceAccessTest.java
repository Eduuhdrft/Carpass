package com.carpass.service_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.ForbiddenOperationException;
import com.carpass.service_api.repository.WorkshopRepository;

/**
 * Quem ve o financeiro.
 *
 * <p>
 * A regra tem duas partes -- o dono sempre ve, o gerente ve se o dono deixar --
 * e essa e a forma mais facil de escrever uma brecha sem perceber. Estes testes
 * cobrem os quatro papeis e os dois estados da chave.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FinanceAccessTest {

	private static final UUID OFICINA = UUID.randomUUID();

	@Mock
	private CurrentUser currentUser;

	@Mock
	private WorkshopRepository workshopRepository;

	private FinanceAccess financeAccess;

	@BeforeEach
	void setUp() {
		financeAccess = new FinanceAccess(currentUser, workshopRepository);
		lenient().when(currentUser.requireWorkshopId()).thenReturn(OFICINA);
	}

	@Test
	@DisplayName("o dono sempre ve, e nem consulta a chave da oficina")
	void ownerAlwaysSees() {
		when(currentUser.hasAnyRole(Role.OWNER)).thenReturn(true);

		assertThat(financeAccess.canView()).isTrue();
		// O dono nao depende da chave: uma ida ao banco aqui seria desperdicio em
		// toda carga do painel.
		verify(workshopRepository, never()).findById(OFICINA);
	}

	@Test
	@DisplayName("o gerente ve quando o dono deixou")
	void managerSeesWhenAllowed() {
		darGerente(true);

		assertThat(financeAccess.canView()).isTrue();
		assertThatCode(() -> financeAccess.requireView()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("o gerente nao ve quando o dono desligou a chave")
	void managerBlockedWhenNotAllowed() {
		darGerente(false);

		assertThat(financeAccess.canView()).isFalse();
		assertThatThrownBy(() -> financeAccess.requireView())
				.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@DisplayName("o gerente sem permissao tambem nao lanca despesa")
	void managerBlockedCannotManage() {
		darGerente(false);

		// Ver e lancar sao metodos diferentes de proposito, mas hoje respondem a
		// mesma regra: quem nao ve o resultado nao mexe no que o compoe.
		assertThatThrownBy(() -> financeAccess.requireManage())
				.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@DisplayName("atendente e mecanico nunca veem, nem com a chave ligada")
	void otherRolesNeverSee() {
		when(currentUser.hasAnyRole(Role.OWNER)).thenReturn(false);
		when(currentUser.hasAnyRole(Role.MANAGER)).thenReturn(false);

		assertThat(financeAccess.canView()).isFalse();
		// A chave e sobre o gerente. Para os demais a resposta sai do papel, sem
		// consultar a oficina.
		verify(workshopRepository, never()).findById(OFICINA);
	}

	@Test
	@DisplayName("so o dono altera quem ve o financeiro")
	void onlyOwnerChangesTheSwitch() {
		when(currentUser.hasAnyRole(Role.OWNER)).thenReturn(false);

		// Se o gerente pudesse alterar, ele se autorizaria sozinho -- e a opcao do
		// dono nao significaria nada.
		assertThatThrownBy(() -> financeAccess.requireOwner())
				.isInstanceOf(ForbiddenOperationException.class);
	}

	private void darGerente(boolean permitido) {
		when(currentUser.hasAnyRole(Role.OWNER)).thenReturn(false);
		when(currentUser.hasAnyRole(Role.MANAGER)).thenReturn(true);

		Workshop oficina = new Workshop();
		oficina.setManagerCanSeeFinance(permitido);
		when(workshopRepository.findById(OFICINA)).thenReturn(Optional.of(oficina));
	}
}
