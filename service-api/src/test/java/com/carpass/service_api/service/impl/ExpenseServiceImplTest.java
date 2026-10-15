package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carpass.service_api.domain.model.Expense;
import com.carpass.service_api.domain.model.ExpenseCategory;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ForbiddenOperationException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.ExpenseCategoryRepository;
import com.carpass.service_api.repository.ExpenseRepository;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.web.dto.expense.ExpenseRequest;

/**
 * Regras de lancamento de despesa.
 *
 * <p>
 * O ponto sensivel aqui e que nada de identidade venha do corpo da requisicao:
 * oficina e responsavel saem do usuario autenticado, e a categoria e conferida
 * contra a oficina antes de ser usada.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

	private static final UUID OFICINA = UUID.randomUUID();
	private static final UUID USUARIO = UUID.randomUUID();
	private static final UUID CATEGORIA = UUID.randomUUID();

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private ExpenseCategoryRepository categoryRepository;

	@Mock
	private WorkshopRepository workshopRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FinanceAccess financeAccess;

	@Mock
	private CurrentUser currentUser;

	private ExpenseServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ExpenseServiceImpl(expenseRepository, categoryRepository, workshopRepository,
				userRepository, financeAccess, currentUser);
		lenient().when(currentUser.requireWorkshopId()).thenReturn(OFICINA);
		lenient().when(currentUser.requireId()).thenReturn(USUARIO);
	}

	@Test
	@DisplayName("lancar grava a oficina e o responsavel do usuario autenticado")
	void createUsesAuthenticatedContext() {
		darCategoria();
		Workshop oficina = new Workshop();
		User autor = new User();
		when(workshopRepository.getReferenceById(OFICINA)).thenReturn(oficina);
		when(userRepository.getReferenceById(USUARIO)).thenReturn(autor);

		service.create(pedido("3000.00", LocalDate.now()));

		ArgumentCaptor<Expense> capturada = ArgumentCaptor.forClass(Expense.class);
		verify(expenseRepository).save(capturada.capture());

		// Nem a oficina nem o autor vieram do corpo da requisicao: aceitar
		// qualquer um dos dois seria entregar o isolamento junto com o formulario.
		assertThat(capturada.getValue().getWorkshop()).isSameAs(oficina);
		assertThat(capturada.getValue().getCreatedBy()).isSameAs(autor);
		assertThat(capturada.getValue().getAmount()).isEqualByComparingTo("3000.00");
	}

	@Test
	@DisplayName("categoria de outra oficina responde 404, e nao 403")
	void categoryFromAnotherWorkshopIsNotFound() {
		when(categoryRepository.findByIdAndWorkshopId(CATEGORIA, OFICINA)).thenReturn(Optional.empty());

		// 403 confirmaria que aquele id existe em algum lugar.
		assertThatThrownBy(() -> service.create(pedido("100.00", LocalDate.now())))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(expenseRepository, never()).save(any());
	}

	@Test
	@DisplayName("despesa com data futura e recusada")
	void rejectsFutureDate() {
		darCategoria();

		// Uma despesa no futuro entraria num mes que ainda nao fechou e
		// distorceria um resultado que ninguem esta olhando ainda.
		assertThatThrownBy(() -> service.create(pedido("100.00", LocalDate.now().plusDays(1))))
				.isInstanceOf(BusinessException.class);
		verify(expenseRepository, never()).save(any());
	}

	@Test
	@DisplayName("editar nao reescreve quem lancou")
	void updateKeepsTheOriginalAuthor() {
		darCategoria();

		User autorOriginal = new User();
		Expense existente = new Expense();
		existente.setCreatedBy(autorOriginal);
		when(expenseRepository.findByIdAndWorkshopId(any(), any())).thenReturn(Optional.of(existente));

		service.update(UUID.randomUUID(), pedido("250.00", LocalDate.now()));

		// createdBy registra quem lancou, nao quem mexeu por ultimo: reescrever
		// apagaria a unica pista de origem do lancamento.
		assertThat(existente.getCreatedBy()).isSameAs(autorOriginal);
		assertThat(existente.getAmount()).isEqualByComparingTo("250.00");
	}

	@Test
	@DisplayName("quem nao pode lancar nao passa nem para a leitura da categoria")
	void blocksWhoCannotManage() {
		doThrow(new ForbiddenOperationException("sem permissao")).when(financeAccess).requireManage();

		assertThatThrownBy(() -> service.create(pedido("100.00", LocalDate.now())))
				.isInstanceOf(ForbiddenOperationException.class);
		verify(categoryRepository, never()).findByIdAndWorkshopId(any(), any());
		verify(expenseRepository, never()).save(any());
	}

	@Test
	@DisplayName("excluir apaga de verdade, e so dentro da propria oficina")
	void deleteRemovesTheRow() {
		Expense existente = new Expense();
		existente.setAmount(new BigDecimal("10.00"));
		when(expenseRepository.findByIdAndWorkshopId(any(), any())).thenReturn(Optional.of(existente));

		service.delete(UUID.randomUUID());

		// Diferente do resto do sistema, aqui excluir apaga: despesa lancada
		// errada precisa sair do resultado, e uma "inativa" que ainda soma seria
		// pior que nao ter o botao.
		verify(expenseRepository).delete(existente);
	}

	@Test
	@DisplayName("excluir despesa de outra oficina responde 404")
	void deleteIsScopedToTheWorkshop() {
		when(expenseRepository.findByIdAndWorkshopId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
				.isInstanceOf(ResourceNotFoundException.class);
		// O tipo no any() e obrigatorio: o repositorio herda delete(T) do
		// CrudRepository e delete(Specification) do JpaSpecificationExecutor, e um
		// any() sem tipo nao decide entre os dois.
		verify(expenseRepository, never()).delete(any(Expense.class));
	}

	// --------------------------------------------------------- apoio
	private void darCategoria() {
		ExpenseCategory categoria = new ExpenseCategory();
		categoria.setName("Aluguel");
		lenient().when(categoryRepository.findByIdAndWorkshopId(CATEGORIA, OFICINA))
				.thenReturn(Optional.of(categoria));
	}

	private ExpenseRequest pedido(String valor, LocalDate data) {
		return new ExpenseRequest("Aluguel do mes", new BigDecimal(valor), data, CATEGORIA, null);
	}
}
