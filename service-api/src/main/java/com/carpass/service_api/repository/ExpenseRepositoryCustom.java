package com.carpass.service_api.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.Expense;

/**
 * Soma de despesas usando o mesmo filtro da listagem.
 *
 * <h2>Por que isto existe</h2>
 *
 * <p>
 * O total do periodo tem que sair do banco e tem que usar <em>exatamente</em> o
 * filtro que a listagem usou. As duas saidas obvias sao ruins:
 * </p>
 *
 * <ul>
 * <li><strong>Somar a pagina recebida</strong> daria um "total" que muda ao
 * virar de pagina.</li>
 * <li><strong>Escrever um {@code @Query} proprio</strong> duplicaria a regra de
 * filtro -- e o filtro tem parametros opcionais, entao a versao JPQL cairia no
 * idioma {@code (:param is null or ...)} que o PostgreSQL recusa.</li>
 * </ul>
 *
 * <p>
 * Reaproveitar a {@code Specification} num {@code SELECT sum(...)} resolve os
 * dois: uma definicao de filtro, uma agregacao no banco.
 * </p>
 */
public interface ExpenseRepositoryCustom {

	/** Soma o valor de todas as despesas que a especificacao alcanca. Nunca nula. */
	BigDecimal sumAmount(Specification<Expense> spec);
}
