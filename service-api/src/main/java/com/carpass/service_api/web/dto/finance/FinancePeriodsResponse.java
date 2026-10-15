package com.carpass.service_api.web.dto.finance;

import java.util.List;

/**
 * Os periodos que fazem sentido oferecer no seletor.
 *
 * <h2>Por que a lista vem do servidor</h2>
 *
 * <p>
 * A tela poderia gerar "os ultimos 24 meses" sozinha, e ofereceria dezenas de
 * periodos vazios de uma oficina que abriu ha tres meses -- cada um deles uma
 * consulta inutil e uma tela em branco. Aqui saem so os anos em que houve
 * entrega ou despesa, mais o ano corrente, que sempre aparece porque e onde a
 * oficina esta lancando agora.
 * </p>
 *
 * @param years  anos com dado, do mais recente para o mais antigo
 * @param months meses oferecidos no modo mensal, do mais recente para o mais
 *               antigo, limitados ao intervalo em que a oficina existe
 */
public record FinancePeriodsResponse(List<Integer> years, List<PeriodOption> months) {

	/**
	 * Um mes selecionavel.
	 *
	 * @param label "Agosto de 2026"
	 */
	public record PeriodOption(int year, int month, String label) {
	}
}
