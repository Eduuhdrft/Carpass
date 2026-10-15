package com.carpass.service_api.web.dto.finance;

import java.util.List;

/**
 * Financeiro de um ano.
 *
 * @param months     os doze meses, sempre. Mes sem movimento vem zerado em vez
 *                   de sumir -- ver {@link MonthlyPoint}
 * @param bestMonth  mes de maior faturamento bruto; nulo quando o ano nao teve
 *                   faturamento nenhum, porque "melhor mes: janeiro, R$ 0,00"
 *                   seria uma resposta inventada
 * @param worstMonth mes de menor faturamento <strong>entre os que tiveram
 *                   movimento</strong>. Incluir os zerados faria o pior mes ser
 *                   sempre um mes que ainda nao chegou
 */
public record AnnualFinanceResponse(int year, FinanceSummary summary, List<MonthlyPoint> months,
		MonthlyPoint bestMonth, MonthlyPoint worstMonth, List<CategoryExpense> byCategory) {
}
