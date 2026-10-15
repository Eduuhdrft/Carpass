package com.carpass.service_api.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.model.Part;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros da busca de pecas.
 *
 * <h2>Por que Specification e nao JPQL</h2>
 *
 * <p>
 * O idioma {@code (:param is null or coluna = :param)} ja custou um 500 em
 * producao na busca de ordens de servico: o PostgreSQL analisa a instrucao antes
 * de receber os valores e, num {@code ? is null} solto, nao tem de onde inferir
 * o tipo do parametro. Aqui filtro ausente nao vira clausula, entao o problema
 * nao existe por construcao.
 * </p>
 *
 * <h2>O join do tipo</h2>
 *
 * <p>
 * O tipo e opcional na peca, entao o predicado usa o id da associacao sem
 * forcar join: peca sem tipo continua na listagem sem filtro, e some quando
 * alguem filtra por um tipo -- que e o esperado.
 * </p>
 */
public final class PartSpecifications {

	public static Specification<Part> filter(UUID workshopId, String term, Boolean active, boolean lowStockOnly,
			UUID partTypeId, String manufacturer) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("workshop").get("id"), workshopId));

			if (active != null) {
				predicates.add(cb.equal(root.get("active"), active));
			}

			if (lowStockOnly) {
				predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStock")));
			}

			if (partTypeId != null) {
				predicates.add(cb.equal(root.get("partType").get("id"), partTypeId));
			}

			// A marca vem escolhida de uma lista montada a partir do proprio
			// estoque, entao a comparacao e exata -- ignorando so a caixa, para
			// "Bosch" e "BOSCH" nao serem duas marcas.
			if (manufacturer != null && !manufacturer.isBlank()) {
				predicates.add(cb.equal(cb.lower(root.get("manufacturer")),
						manufacturer.trim().toLowerCase(Locale.ROOT)));
			}

			if (term != null && !term.isBlank()) {
				String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("code"), "")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("manufacturer"), "")), pattern)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private PartSpecifications() {
		// classe utilitaria
	}
}
