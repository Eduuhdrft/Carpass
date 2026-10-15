package com.carpass.service_api.web.dto.finance;

import java.math.BigDecimal;

/**
 * Um mes na serie temporal.
 *
 * <p>
 * Meses sem movimento <strong>aparecem zerados</strong>, nao somem. Um grafico
 * que pula de marco para maio mente sobre o ritmo da oficina: quem olha ve duas
 * barras vizinhas e le como meses consecutivos. Abril vazio precisa aparecer
 * vazio.
 * </p>
 *
 * @param label rotulo curto para o eixo ("Ago"), montado no servidor
 */
public record MonthlyPoint(int year, int month, String label, BigDecimal grossRevenue, BigDecimal expenses,
		BigDecimal netRevenue, long orderCount) {
}
