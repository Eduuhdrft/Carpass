package com.carpass.service_api.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.ExpenseCategory;

import jakarta.persistence.criteria.Predicate;

/** Filtros da busca de categorias de despesa. */
public final class ExpenseCategorySpecifications {

	public static Specification<ExpenseCategory> filter(UUID workshopId, String term, Boolean active) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("workshop").get("id"), workshopId));

			if (active != null) {
				predicates.add(cb.equal(root.get("active"), active));
			}

			if (term != null && !term.isBlank()) {
				String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private ExpenseCategorySpecifications() {
		// classe utilitaria
	}
}
