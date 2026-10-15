package com.carpass.service_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.repository.ExpenseRepository;
import com.carpass.service_api.repository.WorkOrderRepository;
import com.carpass.service_api.repository.projection.CategoryAmount;
import com.carpass.service_api.repository.projection.MonthlyAmount;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.service.FinanceService;
import com.carpass.service_api.web.dto.finance.AnnualFinanceResponse;
import com.carpass.service_api.web.dto.finance.CategoryExpense;
import com.carpass.service_api.web.dto.finance.FinancePeriodsResponse;
import com.carpass.service_api.web.dto.finance.FinanceSummary;
import com.carpass.service_api.web.dto.finance.MonthlyFinanceResponse;
import com.carpass.service_api.web.dto.finance.MonthlyPoint;

/**
 * Bruto, despesas e liquido de um periodo.
 *
 * <h2>De onde vem cada numero</h2>
 *
 * <ul>
 * <li><strong>Bruto:</strong> soma do total das OS <em>entregues</em>, pela data
 * de entrega. E a regra que o painel ja usava, preservada palavra por palavra --
 * mudar aqui faria o cartao do painel e esta tela discordarem sobre o mesmo mes,
 * e nao haveria como saber qual esta certo.</li>
 * <li><strong>Despesas:</strong> soma dos lancamentos pela data da despesa.</li>
 * <li><strong>Liquido:</strong> a subtracao dos dois, feita em
 * {@link FinanceSummary#of}.</li>
 * </ul>
 *
 * <h2>Quantas consultas cada tela custa</h2>
 *
 * <p>
 * Duas agregacoes por serie mais uma por categoria -- e nao uma consulta por mes.
 * A visao anual pede <em>uma</em> consulta de faturamento e <em>uma</em> de
 * despesa cobrindo os doze meses, agrupadas no banco; a mensal pede o mesmo
 * sobre uma janela de seis meses, que ja contem o mes anterior da comparacao. Um
 * laco de doze consultas funcionaria hoje e apareceria como lentidao no ano em
 * que a oficina tivesse volume.
 * </p>
 *
 * <h2>Meses vazios existem</h2>
 *
 * <p>
 * O banco nao devolve linha para mes sem movimento, e a serie precisa dele: um
 * grafico que pula de marco para maio faz duas barras vizinhas parecerem meses
 * consecutivos. Todas as series aqui sao preenchidas com zero antes de sair.
 * </p>
 */
@Service
public class FinanceServiceImpl implements FinanceService {

	/** Quantos meses de contexto acompanham a visao mensal. */
	private static final int RECENT_MONTHS = 6;

	/** Ate quantos anos para tras oferecer no seletor quando nao ha dado nenhum. */
	private static final int FALLBACK_YEARS = 3;

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final WorkOrderRepository workOrderRepository;
	private final ExpenseRepository expenseRepository;
	private final FinanceAccess financeAccess;
	private final CurrentUser currentUser;

	public FinanceServiceImpl(WorkOrderRepository workOrderRepository, ExpenseRepository expenseRepository,
			FinanceAccess financeAccess, CurrentUser currentUser) {
		this.workOrderRepository = workOrderRepository;
		this.expenseRepository = expenseRepository;
		this.financeAccess = financeAccess;
		this.currentUser = currentUser;
	}

	// ------------------------------------------------------------- mensal
	@Override
	@Transactional(readOnly = true)
	public MonthlyFinanceResponse monthly(int year, int month) {
		financeAccess.requireView();
		UUID workshopId = currentUser.requireWorkshopId();

		YearMonth escolhido = parse(year, month);
		YearMonth inicioDaJanela = escolhido.minusMonths(RECENT_MONTHS - 1L);

		// Uma janela so: ela contem o mes escolhido, o anterior da comparacao e os
		// seis do grafico. Tres perguntas, duas consultas.
		LocalDate de = inicioDaJanela.atDay(1);
		LocalDate ate = escolhido.atEndOfMonth();

		Map<YearMonth, MonthlyAmount> faturamento = indexar(
				workOrderRepository.sumRevenueByMonth(workshopId, de.atStartOfDay(), ate.atTime(LocalTime.MAX)));
		Map<YearMonth, MonthlyAmount> despesas = indexar(expenseRepository.sumByMonth(workshopId, de, ate));

		FinanceSummary resumo = resumoDe(escolhido, faturamento, despesas);
		YearMonth anterior = escolhido.minusMonths(1);
		FinanceSummary resumoAnterior = resumoDe(anterior, faturamento, despesas);

		List<MonthlyPoint> serie = serie(inicioDaJanela, RECENT_MONTHS, faturamento, despesas);

		List<CategoryExpense> porCategoria = porCategoria(workshopId, escolhido.atDay(1), escolhido.atEndOfMonth(),
				resumo.expenses());

		return new MonthlyFinanceResponse(escolhido.getYear(), escolhido.getMonthValue(), rotuloLongo(escolhido),
				resumo, resumoAnterior, rotuloLongo(anterior), serie, porCategoria);
	}

	// -------------------------------------------------------------- anual
	@Override
	@Transactional(readOnly = true)
	public AnnualFinanceResponse annual(int year) {
		financeAccess.requireView();
		UUID workshopId = currentUser.requireWorkshopId();

		int ano = validarAno(year);
		YearMonth janeiro = YearMonth.of(ano, 1);
		LocalDate de = janeiro.atDay(1);
		LocalDate ate = YearMonth.of(ano, 12).atEndOfMonth();

		Map<YearMonth, MonthlyAmount> faturamento = indexar(
				workOrderRepository.sumRevenueByMonth(workshopId, de.atStartOfDay(), ate.atTime(LocalTime.MAX)));
		Map<YearMonth, MonthlyAmount> despesas = indexar(expenseRepository.sumByMonth(workshopId, de, ate));

		List<MonthlyPoint> meses = serie(janeiro, 12, faturamento, despesas);

		BigDecimal brutoDoAno = meses.stream().map(MonthlyPoint::grossRevenue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal despesaDoAno = meses.stream().map(MonthlyPoint::expenses)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		long ordens = meses.stream().mapToLong(MonthlyPoint::orderCount).sum();
		long lancamentos = despesas.values().stream().mapToLong(MonthlyAmount::count).sum();

		FinanceSummary resumo = FinanceSummary.of(brutoDoAno, despesaDoAno, ordens, lancamentos);

		// Extremos apenas entre meses que tiveram faturamento. Com os zerados no
		// meio, o "pior mes" seria sempre um mes que ainda nem chegou.
		List<MonthlyPoint> comMovimento = meses.stream()
				.filter(ponto -> ponto.grossRevenue().signum() > 0)
				.toList();

		MonthlyPoint melhor = comMovimento.stream()
				.max(Comparator.comparing(MonthlyPoint::grossRevenue)).orElse(null);
		MonthlyPoint pior = comMovimento.stream()
				.min(Comparator.comparing(MonthlyPoint::grossRevenue)).orElse(null);

		List<CategoryExpense> porCategoria = porCategoria(workshopId, de, ate, resumo.expenses());

		return new AnnualFinanceResponse(ano, resumo, meses, melhor, pior, porCategoria);
	}

	// ------------------------------------------------------------ periodos
	@Override
	@Transactional(readOnly = true)
	public FinancePeriodsResponse periods() {
		financeAccess.requireView();
		UUID workshopId = currentUser.requireWorkshopId();

		YearMonth agora = YearMonth.now();

		// O ano corrente sempre entra: e onde a oficina esta lancando, mesmo que
		// ainda nao tenha entregue nada.
		Set<Integer> anos = new LinkedHashSet<>();
		anos.add(agora.getYear());
		anos.addAll(workOrderRepository.findYearsWithRevenue(workshopId));
		anos.addAll(expenseRepository.findYearsWithExpenses(workshopId));

		List<Integer> ordenados = anos.stream().sorted(Comparator.reverseOrder()).toList();

		// Oficina sem dado nenhum ainda merece algo no seletor, senao a tela abre
		// com um campo vazio e parece quebrada.
		if (ordenados.size() == 1) {
			List<Integer> completado = new ArrayList<>();
			for (int i = 0; i < FALLBACK_YEARS; i++) {
				completado.add(agora.getYear() - i);
			}
			ordenados = completado;
		}

		int anoMaisAntigo = ordenados.get(ordenados.size() - 1);
		YearMonth primeiro = YearMonth.of(anoMaisAntigo, 1);

		List<FinancePeriodsResponse.PeriodOption> meses = new ArrayList<>();
		for (YearMonth cursor = agora; !cursor.isBefore(primeiro); cursor = cursor.minusMonths(1)) {
			meses.add(new FinancePeriodsResponse.PeriodOption(cursor.getYear(), cursor.getMonthValue(),
					rotuloLongo(cursor)));
		}

		return new FinancePeriodsResponse(ordenados, meses);
	}

	// -------------------------------------------------------------- apoio
	/**
	 * Indexa o resultado do banco por mes.
	 *
	 * <p>
	 * O ano entra na chave de proposito: a janela da visao mensal atravessa a
	 * virada do ano, e sem ele janeiro de 2026 e janeiro de 2025 se somariam em
	 * silencio -- o pior tipo de erro financeiro, o que da um numero plausivel.
	 * </p>
	 */
	private Map<YearMonth, MonthlyAmount> indexar(List<MonthlyAmount> linhas) {
		Map<YearMonth, MonthlyAmount> mapa = new HashMap<>();
		for (MonthlyAmount linha : linhas) {
			mapa.put(YearMonth.of(linha.year(), linha.month()), linha);
		}
		return mapa;
	}

	private FinanceSummary resumoDe(YearMonth mes, Map<YearMonth, MonthlyAmount> faturamento,
			Map<YearMonth, MonthlyAmount> despesas) {
		MonthlyAmount bruto = faturamento.get(mes);
		MonthlyAmount gasto = despesas.get(mes);

		return FinanceSummary.of(
				bruto == null ? BigDecimal.ZERO : bruto.amount(),
				gasto == null ? BigDecimal.ZERO : gasto.amount(),
				bruto == null ? 0L : bruto.count(),
				gasto == null ? 0L : gasto.count());
	}

	/** Serie contigua a partir de um mes, com os vazios preenchidos por zero. */
	private List<MonthlyPoint> serie(YearMonth inicio, int quantidade, Map<YearMonth, MonthlyAmount> faturamento,
			Map<YearMonth, MonthlyAmount> despesas) {
		List<MonthlyPoint> pontos = new ArrayList<>(quantidade);

		for (int i = 0; i < quantidade; i++) {
			YearMonth mes = inicio.plusMonths(i);
			FinanceSummary resumo = resumoDe(mes, faturamento, despesas);
			pontos.add(new MonthlyPoint(mes.getYear(), mes.getMonthValue(), rotuloCurto(mes), resumo.grossRevenue(),
					resumo.expenses(), resumo.netRevenue(), resumo.orderCount()));
		}

		return pontos;
	}

	/**
	 * Despesas por categoria, com a fatia de cada uma.
	 *
	 * <p>
	 * A fatia sai daqui, e nao da tela, porque o divisor e o mesmo total que vai
	 * nos cartoes. Dividido no frontend, uma mudanca na regra do total deixaria os
	 * dois desalinhados sem ninguem notar.
	 * </p>
	 */
	private List<CategoryExpense> porCategoria(UUID workshopId, LocalDate de, LocalDate ate, BigDecimal total) {
		List<CategoryAmount> linhas = expenseRepository.sumByCategory(workshopId, de, ate);
		List<CategoryExpense> resultado = new ArrayList<>(linhas.size());

		for (CategoryAmount linha : linhas) {
			BigDecimal fatia = total == null || total.signum() == 0
					? BigDecimal.ZERO
					: linha.amount().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
			resultado.add(new CategoryExpense(linha.categoryId(), linha.categoryName(), linha.amount(), linha.count(),
					fatia));
		}

		return resultado;
	}

	private YearMonth parse(int year, int month) {
		if (month < 1 || month > 12) {
			throw new BusinessException("Mes invalido. Informe um valor entre 1 e 12.");
		}
		return YearMonth.of(validarAno(year), month);
	}

	/**
	 * Barra ano fora de faixa.
	 *
	 * <p>
	 * Nao e capricho: {@code YearMonth.of} aceita valores absurdos e a consulta
	 * seguiria com um intervalo gigante, varrendo a tabela para devolver zero.
	 * </p>
	 */
	private int validarAno(int year) {
		int anoAtual = LocalDateTime.now().getYear();
		if (year < 2000 || year > anoAtual + 1) {
			throw new BusinessException("Ano invalido para consulta financeira.");
		}
		return year;
	}

	/** "Agosto de 2026". Montado aqui para o nome do mes existir em um lugar so. */
	private String rotuloLongo(YearMonth mes) {
		String nome = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
		return capitalizar(nome) + " de " + mes.getYear();
	}

	/** "Ago" -- rotulo do eixo do grafico. */
	private String rotuloCurto(YearMonth mes) {
		String nome = mes.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
		return capitalizar(nome);
	}

	private String capitalizar(String texto) {
		if (texto == null || texto.isEmpty()) {
			return texto;
		}
		return texto.substring(0, 1).toUpperCase(PT_BR) + texto.substring(1);
	}
}
