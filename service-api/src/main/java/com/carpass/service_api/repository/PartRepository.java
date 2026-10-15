package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.repository.projection.ManufacturerSummary;

/**
 * Pecas em estoque.
 */
public interface PartRepository extends JpaRepository<Part, UUID>, JpaSpecificationExecutor<Part> {

	@EntityGraph(attributePaths = { "partType" })
	Optional<Part> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndCodeIgnoreCase(UUID workshopId, String code);

	boolean existsByWorkshopIdAndCodeIgnoreCaseAndIdNot(UUID workshopId, String code, UUID id);

	/**
	 * Busca com filtros, montada em {@code PartSpecifications}.
	 *
	 * <p>
	 * O {@code EntityGraph} traz o tipo junto: a listagem mostra o nome dele em
	 * toda linha, e sem isto seria uma consulta por peca.
	 * </p>
	 */
	@Override
	@EntityGraph(attributePaths = { "partType" })
	Page<Part> findAll(Specification<Part> spec, Pageable pageable);

	@Query("""
			select count(p) from Part p
			where p.workshop.id = :workshopId and p.active = true and p.stockQuantity <= p.minimumStock
			""")
	long countLowStock(@Param("workshopId") UUID workshopId);

	// ------------------------------------------------------------- marcas
	/*
	 * Duas consultas, em vez de uma com parametro opcional.
	 *
	 * Seria tentador escrever uma so com "(:typeId is null or ...)" -- e e
	 * exatamente esse idioma que derrubou a busca de ordens de servico no
	 * PostgreSQL. Duas consultas explicitas custam algumas linhas e nao tem como
	 * falhar por inferencia de tipo.
	 */

	/** Marcas presentes no estoque, com quantas pecas e quanto saldo cada uma tem. */
	@Query("""
			select new com.carpass.service_api.repository.projection.ManufacturerSummary(
			           p.manufacturer, count(p), coalesce(sum(p.stockQuantity), 0L))
			from Part p
			where p.workshop.id = :workshopId
			  and p.active = true
			  and p.manufacturer is not null
			  and p.manufacturer <> ''
			group by p.manufacturer
			order by p.manufacturer asc
			""")
	List<ManufacturerSummary> findManufacturers(@Param("workshopId") UUID workshopId);

	/** As marcas de um tipo so -- "quais marcas de oleo de motor eu tenho?". */
	@Query("""
			select new com.carpass.service_api.repository.projection.ManufacturerSummary(
			           p.manufacturer, count(p), coalesce(sum(p.stockQuantity), 0L))
			from Part p
			where p.workshop.id = :workshopId
			  and p.active = true
			  and p.manufacturer is not null
			  and p.manufacturer <> ''
			  and p.partType.id = :partTypeId
			group by p.manufacturer
			order by p.manufacturer asc
			""")
	List<ManufacturerSummary> findManufacturersByType(@Param("workshopId") UUID workshopId,
			@Param("partTypeId") UUID partTypeId);
}
