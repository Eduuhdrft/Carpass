package com.carpass.service_api.web.dto.finance;

import java.math.BigDecimal;

/**
 * O resultado de um periodo: bruto, despesas e liquido.
 *
 * <h2>Por que o liquido vem pronto do servidor</h2>
 *
 * <p>
 * Subtrair dois numeros e trivial -- e e justamente por isso que a conta
 * apareceria em tres telas diferentes se o frontend a fizesse. No dia em que a
 * regra mudar (uma despesa que nao entra no resultado, um imposto tratado a
 * parte), tres lugares precisariam mudar juntos, e um deles nao mudaria. O
 * servidor calcula, a tela apresenta.
 * </p>
 *
 * @param grossRevenue faturamento bruto: OS entregues no periodo, pela data de
 *                     entrega -- a mesma regra que o painel ja usava
 * @param expenses     total lancado em despesas no periodo
 * @param netRevenue   {@code grossRevenue - expenses}. Pode ser negativo, e
 *                     mostrar isso e o ponto: mes no vermelho tem que aparecer
 *                     como vermelho, nao como zero
 * @param orderCount   quantas ordens de servico compuseram o bruto
 * @param expenseCount quantos lancamentos compuseram as despesas
 */
public record FinanceSummary(BigDecimal grossRevenue, BigDecimal expenses, BigDecimal netRevenue,
		long orderCount, long expenseCount) {

	public static FinanceSummary of(BigDecimal gross, BigDecimal expenses, long orderCount, long expenseCount) {
		BigDecimal seguroBruto = gross == null ? BigDecimal.ZERO : gross;
		BigDecimal seguroDespesa = expenses == null ? BigDecimal.ZERO : expenses;
		return new FinanceSummary(seguroBruto, seguroDespesa, seguroBruto.subtract(seguroDespesa), orderCount,
				expenseCount);
	}
}
