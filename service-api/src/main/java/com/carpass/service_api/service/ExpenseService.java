package com.carpass.service_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryRequest;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryResponse;
import com.carpass.service_api.web.dto.expense.ExpenseRequest;
import com.carpass.service_api.web.dto.expense.ExpenseResponse;

/**
 * Despesas da oficina e suas categorias.
 */
public interface ExpenseService {

	PageResponse<ExpenseResponse> search(String term, UUID categoryId, LocalDate from, LocalDate to,
			Pageable pageable);

	/**
	 * Total das despesas que o filtro atual alcanca.
	 *
	 * <p>
	 * Vem separado da pagina de proposito: o total do periodo nao pode ser a soma
	 * da pagina que a tela recebeu. Com vinte lancamentos por pagina, somar o que
	 * chegou daria um "total" que muda ao virar a pagina.
	 * </p>
	 */
	BigDecimal total(String term, UUID categoryId, LocalDate from, LocalDate to);

	ExpenseResponse findById(UUID id);

	ExpenseResponse create(ExpenseRequest request);

	ExpenseResponse update(UUID id, ExpenseRequest request);

	/**
	 * Apaga de verdade.
	 *
	 * <p>
	 * Diferente do resto do sistema, onde excluir desativa: despesa lancada errada
	 * precisa sumir do resultado, e uma despesa "inativa" que ainda soma seria pior
	 * que nao ter o botao. Nada depende dela por referencia.
	 * </p>
	 */
	void delete(UUID id);

	// -------------------------------------------------------- categorias
	PageResponse<ExpenseCategoryResponse> searchCategories(String term, Boolean active, Pageable pageable);

	List<ExpenseCategoryResponse> listActiveCategories();

	ExpenseCategoryResponse findCategoryById(UUID id);

	ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request);

	ExpenseCategoryResponse updateCategory(UUID id, ExpenseCategoryRequest request);

	ExpenseCategoryResponse changeCategoryStatus(UUID id, boolean active);

	/** Cria a lista padrao de categorias para uma oficina recem-criada. */
	void seedDefaultCategories(Workshop workshop);
}
