package com.carpass.service_api.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.Expense;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Implementacao do {@link ExpenseRepositoryCustom}.
 *
 * <p>
 * O nome da classe importa: o Spring Data procura por
 * {@code <NomeDoFragmento>Impl}. Renomear sem renomear o fragmento faz o metodo
 * sumir do repositorio, e o erro so aparece na subida da aplicacao.
 * </p>
 */
public class ExpenseRepositoryCustomImpl implements ExpenseRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public BigDecimal sumAmount(Specification<Expense> spec) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
		Root<Expense> root = query.from(Expense.class);

		query.select(cb.sum(root.get("amount")));

		Predicate predicate = spec == null ? null : spec.toPredicate(root, query, cb);
		if (predicate != null) {
			query.where(predicate);
		}

		// Periodo sem lancamento devolve nulo em SQL, nao zero. Traduzir aqui evita
		// que cada chamador se lembre disso -- e esquecer seria um zero virando
		// NullPointerException no meio de um calculo financeiro.
		BigDecimal total = entityManager.createQuery(query).getSingleResult();
		return total == null ? BigDecimal.ZERO : total;
	}
}
