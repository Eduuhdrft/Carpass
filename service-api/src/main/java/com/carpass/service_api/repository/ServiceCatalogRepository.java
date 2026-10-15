package com.carpass.service_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.ServiceCatalog;

/**
 * Catalogo de servicos oferecidos pela oficina.
 */
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {

	Optional<ServiceCatalog> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndNameIgnoreCase(UUID workshopId, String name);

	boolean existsByWorkshopIdAndNameIgnoreCaseAndIdNot(UUID workshopId, String name, UUID id);

	@Query("""
			select s from ServiceCatalog s
			where s.workshop.id = :workshopId
			  and (:active is null or s.active = :active)
			  and (:term is null or :term = ''
			       or lower(s.name) like lower(concat('%', :term, '%'))
			       or lower(coalesce(s.description, '')) like lower(concat('%', :term, '%')))
			""")
	Page<ServiceCatalog> search(@Param("workshopId") UUID workshopId, @Param("term") String term,
			@Param("active") Boolean active, Pageable pageable);
}
