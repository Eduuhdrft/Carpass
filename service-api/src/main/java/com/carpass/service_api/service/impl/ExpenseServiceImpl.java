package com.carpass.service_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.DefaultExpenseCategories;
import com.carpass.service_api.domain.model.Expense;
import com.carpass.service_api.domain.model.ExpenseCategory;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.ExpenseCategoryRepository;
import com.carpass.service_api.repository.ExpenseRepository;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.repository.spec.ExpenseCategorySpecifications;
import com.carpass.service_api.repository.spec.ExpenseSpecifications;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.service.ExpenseService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryRequest;
import com.carpass.service_api.web.dto.expense.ExpenseCategoryResponse;
import com.carpass.service_api.web.dto.expense.ExpenseRequest;
import com.carpass.service_api.web.dto.expense.ExpenseResponse;

/**
 * Regras das despesas.
 *
 * <h2>Toda operacao passa pela guarda do financeiro</h2>
 *
 * <p>
 * A checagem esta no servico, e nao so no controller, porque a regra depende de
 * uma coluna da oficina ("o dono deixou o gerente ver?") e porque um caminho
 * novo -- uma importacao, uma rotina -- herdaria a protecao sem ninguem lembrar
 * de anota-lo.
 * </p>
 *
 * <h2>Nada vem do corpo da requisicao</h2>
 *
 * <p>
 * Oficina e responsavel saem do usuario autenticado. A categoria vem por id, e
 * por isso e resolvida com {@code findByIdAndWorkshopId}: um
 * {@code getReferenceById} aceitaria a categoria de outra oficina sem reclamar,
 * e a despesa nasceria apontando para fora.
 * </p>
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

	private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

	private final ExpenseRepository expenseRepository;
	private final ExpenseCategoryRepository categoryRepository;
	private final WorkshopRepository workshopRepository;
	private final UserRepository userRepository;
	private final FinanceAccess financeAccess;
	private final CurrentUser currentUser;

	public ExpenseServiceImpl(ExpenseRepository expenseRepository, ExpenseCategoryRepository categoryRepository,
			WorkshopRepository workshopRepository, UserRepository userRepository, FinanceAccess financeAccess,
			CurrentUser currentUser) {
		this.expenseRepository = expenseRepository;
		this.categoryRepository = categoryRepository;
		this.workshopRepository = workshopRepository;
		this.userRepository = userRepository;
		this.financeAccess = financeAccess;
		this.currentUser = currentUser;
	}

	// --------------------------------------------------------- despesas
	@Override
	@Transactional(readOnly = true)
	public PageResponse<ExpenseResponse> search(String term, UUID categoryId, LocalDate from, LocalDate to,
			Pageable pageable) {
		financeAccess.requireView();
		return PageResponse.from(expenseRepository.findAll(spec(term, categoryId, from, to), pageable),
				ExpenseResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal total(String term, UUID categoryId, LocalDate from, LocalDate to) {
		financeAccess.requireView();

		// A soma corre no banco, sobre a mesma Specification da listagem. Somar a
		// pagina recebida daria um total que muda ao virar de pagina; carregar
		// tudo para somar em memoria ficaria mais lento a cada lancamento.
		return expenseRepository.sumAmount(spec(term, categoryId, from, to));
	}

	@Override
	@Transactional(readOnly = true)
	public ExpenseResponse findById(UUID id) {
		financeAccess.requireView();
		return ExpenseResponse.from(load(id));
	}

	@Override
	@Transactional
	public ExpenseResponse create(ExpenseRequest request) {
		financeAccess.requireManage();
		UUID workshopId = currentUser.requireWorkshopId();

		Expense expense = new Expense();
		expense.setWorkshop(workshopRepository.getReferenceById(workshopId));
		// Quem lancou fica registrado; a resposta de "quem foi?" nao depende da
		// memoria de ninguem.
		expense.setCreatedBy(userRepository.getReferenceById(currentUser.requireId()));
		apply(request, expense);
		expenseRepository.save(expense);

		log.info("Despesa lancada: despesa={} oficina={} usuario={}", expense.getId(), workshopId,
				currentUser.requireId());
		return ExpenseResponse.from(expense);
	}

	@Override
	@Transactional
	public ExpenseResponse update(UUID id, ExpenseRequest request) {
		financeAccess.requireManage();

		Expense expense = load(id);
		// createdBy nao muda na edicao: ele registra quem lancou, nao quem mexeu
		// por ultimo. Reescrever apagaria a unica pista de origem do lancamento.
		apply(request, expense);
		expenseRepository.save(expense);

		log.info("Despesa atualizada: despesa={} usuario={}", id, currentUser.requireId());
		return ExpenseResponse.from(expense);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		financeAccess.requireManage();

		Expense expense = load(id);
		expenseRepository.delete(expense);

		log.info("Despesa excluida: despesa={} oficina={} usuario={} valor={}", id,
				currentUser.requireWorkshopId(), currentUser.requireId(), expense.getAmount());
	}

	// ------------------------------------------------------- categorias
	@Override
	@Transactional(readOnly = true)
	public PageResponse<ExpenseCategoryResponse> searchCategories(String term, Boolean active, Pageable pageable) {
		financeAccess.requireView();
		UUID workshopId = currentUser.requireWorkshopId();
		Map<UUID, Long> contagem = contagemPorCategoria(workshopId);

		return PageResponse.from(
				categoryRepository.findAll(
						ExpenseCategorySpecifications.filter(workshopId, normalize(term), active), pageable),
				categoria -> ExpenseCategoryResponse.from(categoria,
						contagem.getOrDefault(categoria.getId(), 0L)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExpenseCategoryResponse> listActiveCategories() {
		financeAccess.requireView();
		return categoryRepository
				.findByWorkshopIdAndActiveTrueOrderByNameAsc(currentUser.requireWorkshopId()).stream()
				.map(ExpenseCategoryResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ExpenseCategoryResponse findCategoryById(UUID id) {
		financeAccess.requireView();
		return ExpenseCategoryResponse.from(loadCategory(id));
	}

	@Override
	@Transactional
	public ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request) {
		financeAccess.requireManage();
		UUID workshopId = currentUser.requireWorkshopId();
		String nome = request.name().trim();

		if (categoryRepository.existsByWorkshopIdAndNameIgnoreCase(workshopId, nome)) {
			throw new ConflictException("Ja existe uma categoria de despesa com este nome.");
		}

		ExpenseCategory categoria = new ExpenseCategory();
		categoria.setWorkshop(workshopRepository.getReferenceById(workshopId));
		categoria.setActive(true);
		categoria.setName(nome);
		categoria.setDescription(blankToNull(request.description()));
		categoryRepository.save(categoria);

		log.info("Categoria de despesa criada: categoria={} oficina={}", categoria.getId(), workshopId);
		return ExpenseCategoryResponse.from(categoria);
	}

	@Override
	@Transactional
	public ExpenseCategoryResponse updateCategory(UUID id, ExpenseCategoryRequest request) {
		financeAccess.requireManage();

		ExpenseCategory categoria = loadCategory(id);
		String nome = request.name().trim();

		if (categoryRepository.existsByWorkshopIdAndNameIgnoreCaseAndIdNot(currentUser.requireWorkshopId(), nome,
				id)) {
			throw new ConflictException("Ja existe outra categoria de despesa com este nome.");
		}

		categoria.setName(nome);
		categoria.setDescription(blankToNull(request.description()));
		categoryRepository.save(categoria);

		log.info("Categoria de despesa atualizada: categoria={}", id);
		return ExpenseCategoryResponse.from(categoria);
	}

	/**
	 * Desativa ou reativa. Nunca apaga.
	 *
	 * <p>
	 * A despesa aponta para a categoria, entao apagar quebraria o historico de
	 * todo mes que a usou. Desativada ela some dos seletores e continua
	 * descrevendo o que ja foi lancado.
	 * </p>
	 */
	@Override
	@Transactional
	public ExpenseCategoryResponse changeCategoryStatus(UUID id, boolean active) {
		financeAccess.requireManage();

		ExpenseCategory categoria = loadCategory(id);
		categoria.setActive(active);
		categoryRepository.save(categoria);

		log.info("Categoria de despesa {}: categoria={}", active ? "reativada" : "desativada", id);
		return ExpenseCategoryResponse.from(categoria,
				contagemPorCategoria(currentUser.requireWorkshopId()).getOrDefault(id, 0L));
	}

	@Override
	@Transactional
	public void seedDefaultCategories(Workshop workshop) {
		List<ExpenseCategory> categorias = new ArrayList<>();

		for (DefaultExpenseCategories.Seed seed : DefaultExpenseCategories.all()) {
			ExpenseCategory categoria = new ExpenseCategory();
			categoria.setWorkshop(workshop);
			categoria.setName(seed.name());
			categoria.setDescription(seed.description());
			categoria.setActive(true);
			categorias.add(categoria);
		}

		categoryRepository.saveAll(categorias);
		log.info("Categorias de despesa padrao criadas: oficina={} quantidade={}", workshop.getId(),
				categorias.size());
	}

	// ------------------------------------------------------------ apoio
	private Specification<Expense> spec(String term, UUID categoryId, LocalDate from, LocalDate to) {
		return ExpenseSpecifications.filter(currentUser.requireWorkshopId(), normalize(term), categoryId, from, to);
	}

	private void apply(ExpenseRequest request, Expense expense) {
		if (request.expenseDate().isAfter(LocalDate.now())) {
			// Despesa futura entraria num mes que ainda nao fechou e distorceria o
			// resultado de um periodo que ninguem esta olhando ainda.
			throw new BusinessException("A data da despesa nao pode estar no futuro.");
		}

		expense.setDescription(request.description().trim());
		expense.setAmount(request.amount());
		expense.setExpenseDate(request.expenseDate());
		expense.setCategory(resolveCategory(request.categoryId()));
		expense.setNotes(blankToNull(request.notes()));
	}

	private Expense load(UUID id) {
		return expenseRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Despesa"));
	}

	private ExpenseCategory loadCategory(UUID id) {
		return categoryRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Categoria de despesa"));
	}

	/**
	 * Resolve a categoria conferindo a oficina.
	 *
	 * <p>
	 * O id vem da requisicao, entao <strong>tem</strong> que ser buscado por
	 * {@code findByIdAndWorkshopId}. Categoria de outra oficina responde 404, como
	 * todo recurso alheio -- 403 confirmaria que aquele id existe.
	 * </p>
	 */
	private ExpenseCategory resolveCategory(UUID categoryId) {
		return categoryRepository.findByIdAndWorkshopId(categoryId, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Categoria de despesa"));
	}

	private Map<UUID, Long> contagemPorCategoria(UUID workshopId) {
		Map<UUID, Long> contagem = new HashMap<>();
		for (Object[] linha : categoryRepository.countExpensesByCategory(workshopId)) {
			contagem.put((UUID) linha[0], ((Number) linha[1]).longValue());
		}
		return contagem;
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
