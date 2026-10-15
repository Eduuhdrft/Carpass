package com.carpass.service_api.web.dto.finance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Quanto uma categoria pesou no periodo.
 *
 * @param share fatia do total de despesas, de 0 a 100, com uma casa. Vem
 *              calculada do servidor porque o total dela e o mesmo total dos
 *              cartoes -- se a tela dividisse por conta propria, uma mudanca na
 *              regra do total deixaria os dois desalinhados sem ninguem notar
 */
public record CategoryExpense(UUID categoryId, String categoryName, BigDecimal amount, long count,
		BigDecimal share) {
}
