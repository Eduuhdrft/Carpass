package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ForbiddenOperationException;
import com.carpass.service_api.repository.ExpenseRepository;
import com.carpass.service_api.repository.WorkOrderRepository;
import com.carpass.service_api.repository.projection.CategoryAmount;
import com.carpass.service_api.repository.projection.MonthlyAmount;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.web.dto.finance.AnnualFinanceResponse;
import com.carpass.service_api.web.dto.finance.MonthlyFinanceResponse;

/**
 * Bruto, despesas e liquido.
 *
 * <p>
 * O que estes testes travam e a aritmetica do resultado: e a unica parte do
 * sistema em que um erro nao aparece como erro -- aparece como um numero
 * plausivel, e a oficina toma decisao em cima dele.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FinanceServiceImplTest {

	private static final UUID OFICINA = UUID.randomUUID();

	@Mock
	private WorkOrderRepository workOrderRepository;

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private FinanceAccess financeAccess;

	@Mock
	private CurrentUser currentUser;

	private FinanceServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new FinanceServiceImpl(workOrderRepository, expenseRepository, financeAccess, currentUser);
		lenient().when(currentUser.requireWorkshopId()).thenReturn(OFICINA);
	}

	// --------------------------------------------------------- mensal
	@Test
	@DisplayName("liquido e bruto menos despesas")
	void netIsGrossMinusExpenses() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento(new MonthlyAmount(ano, mes, new BigDecimal("12500.00"), 8));
		darDespesas(new MonthlyAmount(ano, mes, new BigDecimal("4300.50"), 6));

		MonthlyFinanceResponse resposta = service.monthly(ano, mes);

		assertThat(resposta.summary().grossRevenue()).isEqualByComparingTo("12500.00");
		assertThat(resposta.summary().expenses()).isEqualByComparingTo("4300.50");
		assertThat(resposta.summary().netRevenue()).isEqualByComparingTo("8199.50");
		assertThat(resposta.summary().orderCount()).isEqualTo(8);
		assertThat(resposta.summary().expenseCount()).isEqualTo(6);
	}

	@Test
	@DisplayName("mes sem movimento nenhum devolve zero, e nao nulo")
	void emptyMonthIsZero() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento();
		darDespesas();

		MonthlyFinanceResponse resposta = service.monthly(ano, mes);

		// Zero e um resultado; nulo seria uma tela quebrada.
		assertThat(resposta.summary().grossRevenue()).isEqualByComparingTo("0");
		assertThat(resposta.summary().expenses()).isEqualByComparingTo("0");
		assertThat(resposta.summary().netRevenue()).isEqualByComparingTo("0");
		assertThat(resposta.summary().orderCount()).isZero();
	}

	@Test
	@DisplayName("mes que gastou mais do que faturou fica negativo")
	void netCanBeNegative() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento(new MonthlyAmount(ano, mes, new BigDecimal("1000.00"), 1));
		darDespesas(new MonthlyAmount(ano, mes, new BigDecimal("2500.00"), 4));

		// Prejuizo tem que sair como prejuizo. Cortar em zero esconderia
		// exatamente o mes que a pessoa precisa ver.
		assertThat(service.monthly(ano, mes).summary().netRevenue()).isEqualByComparingTo("-1500.00");
	}

	@Test
	@DisplayName("os centavos sobrevivem a subtracao")
	void centsSurvive() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento(new MonthlyAmount(ano, mes, new BigDecimal("1000.01"), 1));
		darDespesas(new MonthlyAmount(ano, mes, new BigDecimal("0.02"), 1));

		// BigDecimal o tempo todo: com double isto daria 1000.0099999999999.
		assertThat(service.monthly(ano, mes).summary().netRevenue()).isEqualByComparingTo("999.99");
	}

	@Test
	@DisplayName("a comparacao pega o mes anterior, mesmo virando o ano")
	void previousMonthCrossesTheYear() {
		darFaturamento(
				new MonthlyAmount(2025, 12, new BigDecimal("9000.00"), 5),
				new MonthlyAmount(2026, 1, new BigDecimal("11000.00"), 7));
		darDespesas();

		MonthlyFinanceResponse resposta = service.monthly(2026, 1);

		assertThat(resposta.summary().grossRevenue()).isEqualByComparingTo("11000.00");
		// Janeiro de 2026 compara com dezembro de 2025, e nao com dezembro de 2026
		// nem com "janeiro" sem ano -- que somaria anos diferentes em silencio.
		assertThat(resposta.previous().grossRevenue()).isEqualByComparingTo("9000.00");
		assertThat(resposta.previousLabel()).isEqualTo("Dezembro de 2025");
	}

	@Test
	@DisplayName("a serie mensal traz seis meses, com os vazios zerados")
	void monthlySeriesFillsGaps() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento(new MonthlyAmount(ano, mes, new BigDecimal("500.00"), 1));
		darDespesas();

		MonthlyFinanceResponse resposta = service.monthly(ano, mes);

		assertThat(resposta.recentMonths()).hasSize(6);
		// Mes sem movimento precisa aparecer zerado: sumindo, dois meses nao
		// consecutivos virariam barras vizinhas no grafico.
		assertThat(resposta.recentMonths()).allSatisfy(
				ponto -> assertThat(ponto.grossRevenue()).isNotNull());
		assertThat(resposta.recentMonths().get(5).grossRevenue()).isEqualByComparingTo("500.00");
	}

	@Test
	@DisplayName("a fatia de cada categoria e calculada sobre o total do periodo")
	void categoryShareUsesPeriodTotal() {
		int ano = YearMonth.now().getYear();
		int mes = YearMonth.now().getMonthValue();

		darFaturamento();
		darDespesas(new MonthlyAmount(ano, mes, new BigDecimal("1000.00"), 3));
		when(expenseRepository.sumByCategory(eq(OFICINA), any(), any())).thenReturn(List.of(
				new CategoryAmount(UUID.randomUUID(), "Aluguel", new BigDecimal("750.00"), 1),
				new CategoryAmount(UUID.randomUUID(), "Energia eletrica", new BigDecimal("250.00"), 2)));

		MonthlyFinanceResponse resposta = service.monthly(ano, mes);

		assertThat(resposta.byCategory()).extracting(c -> c.share().toString())
				.containsExactly("75.0", "25.0");
	}

	// ---------------------------------------------------------- anual
	@Test
	@DisplayName("o ano soma os doze meses e devolve todos, inclusive os vazios")
	void annualSumsTwelveMonths() {
		darFaturamento(
				new MonthlyAmount(2026, 3, new BigDecimal("1000.00"), 2),
				new MonthlyAmount(2026, 7, new BigDecimal("3000.00"), 5));
		darDespesas(new MonthlyAmount(2026, 3, new BigDecimal("400.00"), 1));

		AnnualFinanceResponse resposta = service.annual(2026);

		assertThat(resposta.months()).hasSize(12);
		assertThat(resposta.summary().grossRevenue()).isEqualByComparingTo("4000.00");
		assertThat(resposta.summary().expenses()).isEqualByComparingTo("400.00");
		assertThat(resposta.summary().netRevenue()).isEqualByComparingTo("3600.00");
		assertThat(resposta.summary().orderCount()).isEqualTo(7);
	}

	@Test
	@DisplayName("melhor e pior mes ignoram os meses sem movimento")
	void extremesIgnoreEmptyMonths() {
		darFaturamento(
				new MonthlyAmount(2026, 3, new BigDecimal("1000.00"), 2),
				new MonthlyAmount(2026, 7, new BigDecimal("3000.00"), 5));
		darDespesas();

		AnnualFinanceResponse resposta = service.annual(2026);

		assertThat(resposta.bestMonth().month()).isEqualTo(7);
		// Sem este cuidado, o "pior mes" seria sempre janeiro com R$ 0,00 -- ou
		// seja, um mes que ainda nem chegou.
		assertThat(resposta.worstMonth().month()).isEqualTo(3);
	}

	@Test
	@DisplayName("ano sem faturamento nenhum nao inventa melhor mes")
	void emptyYearHasNoBestMonth() {
		darFaturamento();
		darDespesas();

		AnnualFinanceResponse resposta = service.annual(2026);

		assertThat(resposta.bestMonth()).isNull();
		assertThat(resposta.worstMonth()).isNull();
		assertThat(resposta.summary().netRevenue()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("o ano inteiro custa duas consultas, nao vinte e quatro")
	void annualUsesGroupedQueries() {
		darFaturamento();
		darDespesas();

		service.annual(2026);

		// Um laco de doze meses funcionaria hoje e apareceria como lentidao no
		// ano em que a oficina tivesse volume.
		verify(workOrderRepository, times(1)).sumRevenueByMonth(eq(OFICINA), any(), any());
		verify(expenseRepository, times(1)).sumByMonth(eq(OFICINA), any(), any());
	}

	// ------------------------------------------------------ permissao
	@Test
	@DisplayName("quem nao pode ver o financeiro nao chega aos numeros")
	void blocksWhoCannotView() {
		org.mockito.Mockito.doThrow(new ForbiddenOperationException("sem permissao"))
				.when(financeAccess).requireView();

		assertThatThrownBy(() -> service.monthly(2026, 8)).isInstanceOf(ForbiddenOperationException.class);
		assertThatThrownBy(() -> service.annual(2026)).isInstanceOf(ForbiddenOperationException.class);
		assertThatThrownBy(() -> service.periods()).isInstanceOf(ForbiddenOperationException.class);
	}

	// -------------------------------------------------------- entrada
	@Test
	@DisplayName("mes fora de 1 a 12 e recusado antes de tocar o banco")
	void rejectsInvalidMonth() {
		assertThatThrownBy(() -> service.monthly(2026, 0)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> service.monthly(2026, 13)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("ano absurdo e recusado antes de varrer a tabela")
	void rejectsAbsurdYear() {
		// Sem esta guarda, YearMonth.of aceita o valor e a consulta segue com um
		// intervalo gigante para devolver zero.
		assertThatThrownBy(() -> service.annual(1500)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> service.annual(9999)).isInstanceOf(BusinessException.class);
	}

	// --------------------------------------------------------- apoio
	private void darFaturamento(MonthlyAmount... linhas) {
		when(workOrderRepository.sumRevenueByMonth(eq(OFICINA), any(), any())).thenReturn(List.of(linhas));
	}

	private void darDespesas(MonthlyAmount... linhas) {
		when(expenseRepository.sumByMonth(eq(OFICINA), any(LocalDate.class), any(LocalDate.class)))
				.thenReturn(List.of(linhas));
		lenient().when(expenseRepository.sumByCategory(eq(OFICINA), any(), any())).thenReturn(List.of());
	}
}
