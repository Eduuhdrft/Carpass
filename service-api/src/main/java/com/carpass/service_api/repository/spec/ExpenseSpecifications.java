package com.carpass.service_api.repository.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.Expense;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros da listagem de despesas.
 *
 * <p>
 * `Specification` e nao JPQL pelo motivo de sempre neste projeto: o idioma
 * {@code (:param is null or ...)} vira placeholder sem tipo e o PostgreSQL
 * recusa a instrucao inteira. Aqui isso pesa mais que nas outras telas -- os
 * filtros sao <strong>datas</strong>, e foi exatamente com {@code LocalDateTime}
 * que a busca de ordens de servico caiu em producao.
 * </p>
 */
public final class ExpenseSpecifications {

	public static Specification<Expense> filter(UUID workshopId, String term, UUID categoryId, LocalDate from,
			LocalDate to) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("workshop").get("id"), workshopId));

			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("id"), categoryId));
			}

			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("expenseDate"), from));
			}

			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("expenseDate"), to));
			}

			if (term != null && !term.isBlank()) {
				String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("description")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern),
						cb.like(cb.lower(root.get("category").<String>get("name")), pattern)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private ExpenseSpecifications() {
		// classe utilitaria
	}
}
