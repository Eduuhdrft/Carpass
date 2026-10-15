package com.carpass.service_api.repository.projection;

import java.math.BigDecimal;

/**
 * Um mes com um valor somado, direto do banco.
 *
 * <p>
 * O ano vem junto porque a mesma consulta atende a visao mensal (que precisa do
 * mes anterior, as vezes em dezembro do ano passado) e a anual. Sem o ano,
 * janeiro de 2026 e janeiro de 2025 se somariam em silencio.
 * </p>
 *
 * @param year   ano com quatro digitos
 * @param month  1 a 12
 * @param amount total do mes; nunca nulo -- meses sem movimento simplesmente nao
 *               aparecem no resultado
 * @param count  quantos registros compuseram o total
 */
public record MonthlyAmount(int year, int month, BigDecimal amount, long count) {
}
