package com.carpass.service_api.web.dto.finance;

import java.util.List;

/**
 * Financeiro de um mes.
 *
 * @param label         "Agosto de 2026", montado no servidor para o nome do mes
 *                      nao ser escrito por extenso em cada tela que o mostra
 * @param summary       bruto, despesas e liquido <strong>apenas</strong> do mes
 *                      escolhido -- os cartoes nao misturam periodo
 * @param previous      o mesmo do mes anterior, para a comparacao. Vem sempre,
 *                      mesmo zerado: "caiu 100%" e informacao; "sem dados" seria
 *                      uma omissao
 * @param recentMonths  os seis meses terminando no escolhido, como contexto do
 *                      grafico. Sai da mesma consulta que ja traz o mes anterior,
 *                      entao nao custa uma ida a mais ao banco
 * @param byCategory    despesas do mes por categoria, da maior para a menor
 */
public record MonthlyFinanceResponse(int year, int month, String label, FinanceSummary summary,
		FinanceSummary previous, String previousLabel, List<MonthlyPoint> recentMonths,
		List<CategoryExpense> byCategory) {
}
