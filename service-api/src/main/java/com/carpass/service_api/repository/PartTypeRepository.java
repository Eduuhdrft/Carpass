package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.PartType;

/**
 * Tipos de peca da oficina.
 */
public interface PartTypeRepository extends JpaRepository<PartType, UUID>, JpaSpecificationExecutor<PartType> {

	Optional<PartType> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndNameIgnoreCase(UUID workshopId, String name);

	boolean existsByWorkshopIdAndNameIgnoreCaseAndIdNot(UUID workshopId, String name, UUID id);

	/**
	 * Lista para preencher seletor e filtro.
	 *
	 * <p>
	 * So os ativos: um tipo desativado continua valendo nas pecas que ja o usam,
	 * mas nao deve ser oferecido para novas.
	 * </p>
	 */
	List<PartType> findByWorkshopIdAndActiveTrueOrderByNameAsc(UUID workshopId);

	/**
	 * Quantas pecas ativas usam cada tipo.
	 *
	 * <p>
	 * Serve para a tela dizer o que o tipo representa no estoque antes de alguem
	 * desativa-lo. Devolve pares {@code [idDoTipo, quantidade]}; tipo sem peca
	 * nenhuma simplesmente nao aparece.
	 * </p>
	 */
	@Query("""
			select p.partType.id, count(p)
			from Part p
			where p.workshop.id = :workshopId and p.active = true and p.partType is not null
			group by p.partType.id
			""")
	List<Object[]> countActivePartsByType(@Param("workshopId") UUID workshopId);

	/**
	 * Busca paginada.
	 *
	 * <p>
	 * Escrita com {@code Specification} em {@code PartTypeSpecifications} pelo
	 * mesmo motivo da busca de pecas: parametro nulo em JPQL vira placeholder sem
	 * tipo e o PostgreSQL recusa a instrucao inteira.
	 * </p>
	 */
	@Override
	Page<PartType> findAll(Specification<PartType> spec, Pageable pageable);
}
