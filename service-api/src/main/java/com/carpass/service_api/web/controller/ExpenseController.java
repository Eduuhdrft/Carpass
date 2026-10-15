package com.carpass.service_api.web.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.ExpenseService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryRequest;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryResponse;
import com.carpass.service_api.web.dto.expense.ExpenseRequest;
import com.carpass.service_api.web.dto.expense.ExpenseResponse;

import jakarta.validation.Valid;

/**
 * Despesas da oficina e suas categorias.
 *
 * <p>
 * O {@code @PreAuthorize} da classe corta atendente e mecanico. A distincao
 * entre dono e gerente fica no servico, onde a opcao da oficina esta ao alcance
 * -- e onde qualquer caminho novo herda a protecao sem depender de alguem
 * lembrar de anotar.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/expenses")
@PreAuthorize(RoleExpressions.MANAGEMENT)
public class ExpenseController {

	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	// --------------------------------------------------------- despesas
	/**
	 * Lista paginada, filtrada por texto, categoria e periodo.
	 *
	 * @param from primeiro dia considerado (inclusive)
	 * @param to   ultimo dia considerado (inclusive)
	 */
	@GetMapping
	public ResponseEntity<PageResponse<ExpenseResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(expenseService.search(q, categoryId, from, to, pageable));
	}

	/**
	 * Total das despesas que o filtro atual alcanca.
	 *
	 * <p>
	 * Endpoint proprio porque o total nao pertence a pagina: com vinte
	 * lancamentos por vez, somar o que chegou daria um numero que muda ao virar a
	 * pagina.
	 * </p>
	 */
	@GetMapping("/total")
	public ResponseEntity<Map<String, BigDecimal>> total(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ResponseEntity.ok(Map.of("total", expenseService.total(q, categoryId, from, to)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ExpenseResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(expenseService.findById(id));
	}

	@PostMapping
	public ResponseEntity<ExpenseResponse> create(@RequestBody @Valid ExpenseRequest request) {
		ExpenseResponse created = expenseService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/expenses/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ExpenseResponse> update(@PathVariable UUID id,
			@RequestBody @Valid ExpenseRequest request) {
		return ResponseEntity.ok(expenseService.update(id, request));
	}

	/**
	 * Exclui de verdade.
	 *
	 * <p>
	 * Diferente do resto do sistema, onde {@code DELETE} desativa: despesa lancada
	 * errada precisa sair do resultado. Uma despesa "inativa" que ainda soma seria
	 * pior que nao ter o botao.
	 * </p>
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		expenseService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// ------------------------------------------------------- categorias
	@GetMapping("/categories")
	public ResponseEntity<PageResponse<ExpenseCategoryResponse>> listCategories(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(expenseService.searchCategories(q, active, pageable));
	}

	/** Lista completa das ativas, sem paginacao, para seletor e filtro. */
	@GetMapping("/categories/active")
	public ResponseEntity<List<ExpenseCategoryResponse>> listActiveCategories() {
		return ResponseEntity.ok(expenseService.listActiveCategories());
	}

	@GetMapping("/categories/{id}")
	public ResponseEntity<ExpenseCategoryResponse> findCategoryById(@PathVariable UUID id) {
		return ResponseEntity.ok(expenseService.findCategoryById(id));
	}

	@PostMapping("/categories")
	public ResponseEntity<ExpenseCategoryResponse> createCategory(
			@RequestBody @Valid ExpenseCategoryRequest request) {
		ExpenseCategoryResponse created = expenseService.createCategory(request);
		return ResponseEntity.created(URI.create("/api/v1/expenses/categories/" + created.id())).body(created);
	}

	@PutMapping("/categories/{id}")
	public ResponseEntity<ExpenseCategoryResponse> updateCategory(@PathVariable UUID id,
			@RequestBody @Valid ExpenseCategoryRequest request) {
		return ResponseEntity.ok(expenseService.updateCategory(id, request));
	}

	@PatchMapping("/categories/{id}/status")
	public ResponseEntity<ExpenseCategoryResponse> changeCategoryStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(expenseService.changeCategoryStatus(id, request.active()));
	}

	/** Desativa a categoria. Os lancamentos que a usam continuam intactos. */
	@DeleteMapping("/categories/{id}")
	public ResponseEntity<ExpenseCategoryResponse> deactivateCategory(@PathVariable UUID id) {
		return ResponseEntity.ok(expenseService.changeCategoryStatus(id, false));
	}
}
