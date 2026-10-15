package com.carpass.service_api.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.PartType;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros da busca de tipos de peca.
 *
 * <p>
 * Mesmo motivo da busca de ordens de servico: em JPQL, um filtro opcional vira
 * {@code (:param is null or ...)}, e o PostgreSQL analisa a instrucao antes de
 * receber os valores -- num {@code ? is null} solto nao ha de onde inferir o
 * tipo, e a consulta cai inteira. Aqui, filtro ausente simplesmente nao vira
 * clausula.
 * </p>
 */
public final class PartTypeSpecifications {

	public static Specification<PartType> filter(UUID workshopId, String term, Boolean active) {
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

	private PartTypeSpecifications() {
		// classe utilitaria
	}
}
