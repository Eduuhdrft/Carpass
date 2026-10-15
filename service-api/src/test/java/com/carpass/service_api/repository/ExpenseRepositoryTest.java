package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.model.Expense;
import com.carpass.service_api.domain.model.ExpenseCategory;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.repository.projection.CategoryAmount;
import com.carpass.service_api.repository.projection.MonthlyAmount;
import com.carpass.service_api.repository.spec.ExpenseSpecifications;

/**
 * Despesas: filtro por periodo, soma no banco e isolamento entre oficinas.
 *
 * <p>
 * O cenario tem duas oficinas de proposito. Um filtro financeiro que atravessa
 * a fronteira e o pior tipo de vazamento: ele nao da erro, so devolve um numero
 * maior -- e ninguem desconfia de um faturamento que subiu.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {

	@Autowired
	private WorkshopRepository workshopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ExpenseCategoryRepository categoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	private Workshop oficina;
	private Workshop vizinha;
	private ExpenseCategory aluguel;
	private ExpenseCategory energia;

	@BeforeEach
	void setUp() {
		oficina = criarOficina("Oficina de Teste", "11111111000191", "dono@teste.com");
		vizinha = criarOficina("Oficina Vizinha", "22222222000192", "vizinho@teste.com");

		aluguel = criarCategoria(oficina, "Aluguel");
		energia = criarCategoria(oficina, "Energia eletrica");

		criarDespesa(oficina, aluguel, "Aluguel de marco", "3000.00", LocalDate.of(2026, 3, 5));
		criarDespesa(oficina, energia, "Luz de marco", "780.50", LocalDate.of(2026, 3, 12));
		criarDespesa(oficina, energia, "Luz de abril", "820.00", LocalDate.of(2026, 4, 10));
		criarDespesa(oficina, aluguel, "Aluguel de abril", "3000.00", LocalDate.of(2026, 4, 5));

        // A vizinha gasta muito no mesmo mes: se o escopo falhasse, o total da
        // primeira apareceria inflado.
		ExpenseCategory daVizinha = criarCategoria(vizinha, "Aluguel");
		criarDespesa(vizinha, daVizinha, "Aluguel da vizinha", "99000.00", LocalDate.of(2026, 3, 5));
	}

	@Test
	@DisplayName("a soma do periodo sai do banco e respeita o filtro")
	void sumsWithinThePeriod() {
		var filtro = ExpenseSpecifications.filter(oficina.getId(), null, null, LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 3, 31));

		assertThat(expenseRepository.sumAmount(filtro)).isEqualByComparingTo("3780.50");
	}

	@Test
	@DisplayName("periodo sem lancamento soma zero, e nao nulo")
	void emptyPeriodSumsZero() {
		var filtro = ExpenseSpecifications.filter(oficina.getId(), null, null, LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 1, 31));

		// Em SQL, sum() de nada e NULL. Traduzir para zero no repositorio evita
		// que cada chamador lembre disso -- e esquecer seria um NullPointer no
		// meio de um calculo financeiro.
		assertThat(expenseRepository.sumAmount(filtro)).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("a soma nao atravessa a fronteira da oficina")
	void sumDoesNotLeakAcrossWorkshops() {
		var daPrimeira = ExpenseSpecifications.filter(oficina.getId(), null, null, LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 3, 31));
		var daVizinha = ExpenseSpecifications.filter(vizinha.getId(), null, null, LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 3, 31));

		assertThat(expenseRepository.sumAmount(daPrimeira)).isEqualByComparingTo("3780.50");
		assertThat(expenseRepository.sumAmount(daVizinha)).isEqualByComparingTo("99000.00");
	}

	@Test
	@DisplayName("filtrar por categoria estreita a soma e a lista")
	void filtersByCategory() {
		var filtro = ExpenseSpecifications.filter(oficina.getId(), null, energia.getId(), null, null);

		assertThat(expenseRepository.sumAmount(filtro)).isEqualByComparingTo("1600.50");
		assertThat(expenseRepository.findAll(filtro, PageRequest.of(0, 20)).getContent())
				.extracting(Expense::getDescription)
				.containsExactlyInAnyOrder("Luz de marco", "Luz de abril");
	}

	@Test
	@DisplayName("sem datas, o filtro pega tudo da oficina")
	void nullDatesMeanEverything() {
		var filtro = ExpenseSpecifications.filter(oficina.getId(), null, null, null, null);

		// Este e o caso que o idioma "(:param is null or ...)" derrubava no
		// PostgreSQL: a listagem mais comum, sem filtro nenhum.
		assertThat(expenseRepository.sumAmount(filtro)).isEqualByComparingTo("7600.50");
	}

	@Test
	@DisplayName("as datas de borda entram no periodo")
	void boundariesAreInclusive() {
		var soODiaCinco = ExpenseSpecifications.filter(oficina.getId(), null, null, LocalDate.of(2026, 3, 5),
				LocalDate.of(2026, 3, 5));

		assertThat(expenseRepository.sumAmount(soODiaCinco)).isEqualByComparingTo("3000.00");
	}

	@Test
	@DisplayName("a soma por mes agrupa no banco e separa os meses")
	void groupsByMonth() {
		var meses = expenseRepository.sumByMonth(oficina.getId(), LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 12, 31));

		assertThat(meses).extracting(MonthlyAmount::month).containsExactly(3, 4);
		assertThat(meses).extracting(m -> m.amount().toString())
				.containsExactly("3780.50", "3820.00");
	}

	@Test
	@DisplayName("a soma por categoria vem da maior para a menor")
	void groupsByCategory() {
		var categorias = expenseRepository.sumByCategory(oficina.getId(), LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 4, 30));

		assertThat(categorias).extracting(CategoryAmount::categoryName)
				.containsExactly("Aluguel", "Energia eletrica");
		assertThat(categorias.get(0).amount()).isEqualByComparingTo("6000.00");
	}

	@Test
	@DisplayName("os anos com lancamento saem do mais recente para o mais antigo")
	void listsYearsWithExpenses() {
		criarDespesa(oficina, aluguel, "Aluguel antigo", "2800.00", LocalDate.of(2025, 11, 5));

		assertThat(expenseRepository.findYearsWithExpenses(oficina.getId())).containsExactly(2026, 2025);
	}

	// ------------------------------------------------------------- fixtures
	private Workshop criarOficina(String nome, String cnpj, String emailDoDono) {
		User dono = new User();
		dono.setFirstName("Dono");
		dono.setLastName(nome);
		dono.setEmail(emailDoDono);
		dono.setPassword("hash-irrelevante-para-este-teste");
		dono.setActive(true);
		userRepository.save(dono);

		Workshop criada = new Workshop();
		criada.setTradeName(nome);
		criada.setCnpj(cnpj);
		criada.setActive(true);
		criada.setOwner(dono);
		workshopRepository.save(criada);

		return criada;
	}

	private ExpenseCategory criarCategoria(Workshop dona, String nome) {
		ExpenseCategory categoria = new ExpenseCategory();
		categoria.setWorkshop(dona);
		categoria.setName(nome);
		categoria.setActive(true);
		return categoryRepository.save(categoria);
	}

	private Expense criarDespesa(Workshop dona, ExpenseCategory categoria, String descricao, String valor,
			LocalDate data) {
		Expense despesa = new Expense();
		despesa.setWorkshop(dona);
		despesa.setCategory(categoria);
		despesa.setDescription(descricao);
		despesa.setAmount(new BigDecimal(valor));
		despesa.setExpenseDate(data);
		return expenseRepository.save(despesa);
	}
}
