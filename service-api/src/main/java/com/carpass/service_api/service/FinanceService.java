package com.carpass.service_api.service;

import com.carpass.service_api.web.dto.finance.AnnualFinanceResponse;
import com.carpass.service_api.web.dto.finance.FinancePeriodsResponse;
import com.carpass.service_api.web.dto.finance.MonthlyFinanceResponse;

/**
 * O resultado financeiro da oficina.
 *
 * <p>
 * Todo calculo importante vive aqui: bruto, despesas e liquido saem prontos, e a
 * tela so apresenta. Se a subtracao morasse no frontend, ela apareceria em tres
 * telas e no dia em que a regra mudasse uma delas ficaria para tras.
 * </p>
 */
public interface FinanceService {

	/** Um mes: cartoes do proprio mes, comparacao com o anterior e contexto recente. */
	MonthlyFinanceResponse monthly(int year, int month);

	/** Um ano: consolidado, os doze meses e os extremos. */
	AnnualFinanceResponse annual(int year);

	/** O que oferecer no seletor de periodo. */
	FinancePeriodsResponse periods();
}
