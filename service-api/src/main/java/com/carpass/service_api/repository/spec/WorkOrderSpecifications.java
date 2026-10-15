package com.carpass.service_api.repository.spec;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.WorkOrder;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros da listagem de ordens de servico, montados dinamicamente.
 *
 * <h2>Por que nao e uma consulta JPQL com {@code :param is null}</h2>
 *
 * <p>
 * O idioma <code>(:param is null or coluna = :param)</code> parece pratico, mas
 * gera SQL com um placeholder solto em <code>? is null</code>. O PostgreSQL
 * analisa a instrucao antes de receber os valores, e nessa posicao nao existe
 * nada de onde inferir o tipo do parametro. O resultado e o erro
 * <em>"nao foi possivel determinar o tipo de dados do parametro $N"</em>, que
 * derruba a consulta inteira com 500 assim que um dos filtros opcionais vai
 * nulo -- ou seja, no caso mais comum de todos: a listagem sem filtro nenhum.
 * </p>
 *
 * <p>
 * Aqui o problema deixa de existir por construcao: filtro ausente nao vira
 * clausula. Nenhum parametro nulo chega ao banco, entao nao ha o que tipar.
 * </p>
 *
 * <h2>Efeito colateral bem-vindo nos joins</h2>
 *
 * <p>
 * Como a condicao de mecanico so entra quando ha um mecanico escolhido, o join
 * correspondente tambem so aparece nesse caso. Sem filtro, nada e juntado e as
 * ordens ainda sem mecanico definido continuam na lista -- que era exatamente o
 * comportamento esperado.
 * </p>
 */
public final class WorkOrderSpecifications {

	private WorkOrderSpecifications() {
	}

	/**
	 * Monta o filtro da listagem.
	 *
	 * <p>
	 * O escopo por oficina e a unica condicao obrigatoria, e ela nunca depende de
	 * entrada do usuario: vem sempre do usuario autenticado.
	 * </p>
	 *
	 * @param workshopId oficina dona dos dados; obrigatorio
	 * @param term       texto livre para numero da OS, cliente, placa ou modelo
	 * @param from       limite inferior de abertura, ja convertido para o inicio do
	 *                   dia
	 * @param to         limite superior de abertura, ja convertido para o fim do dia
	 */
	public static Specification<WorkOrder> filter(UUID workshopId, String term, WorkOrderStatus status,
			UUID mechanicId, UUID customerId, UUID vehicleId, LocalDateTime from, LocalDateTime to) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			predicates.add(cb.equal(root.get("workshop").get("id"), workshopId));

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (mechanicId != null) {
				predicates.add(cb.equal(root.get("assignedMechanic").get("id"), mechanicId));
			}
			if (customerId != null) {
				predicates.add(cb.equal(root.get("customer").get("id"), customerId));
			}
			if (vehicleId != null) {
				predicates.add(cb.equal(root.get("vehicle").get("id"), vehicleId));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("openedAt"), from));
			}
			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("openedAt"), to));
			}

			if (term != null && !term.isBlank()) {
				String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("customer").<String>get("name")), pattern),
						cb.like(cb.lower(root.get("vehicle").<String>get("plate")), pattern),
						cb.like(cb.lower(root.get("vehicle").<String>get("model")), pattern),
						cb.like(root.get("number").as(String.class), pattern)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
