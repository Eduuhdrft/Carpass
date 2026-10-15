package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.Vehicle;

/**
 * Veiculos atendidos pela oficina.
 */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	Optional<Vehicle> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndPlateIgnoreCase(UUID workshopId, String plate);

	boolean existsByWorkshopIdAndPlateIgnoreCaseAndIdNot(UUID workshopId, String plate, UUID id);

	List<Vehicle> findByWorkshopIdAndCustomerIdOrderByPlateAsc(UUID workshopId, UUID customerId);

	long countByWorkshopIdAndActiveTrue(UUID workshopId);

	/** Busca por placa, marca, modelo ou nome do cliente. */
	@EntityGraph(attributePaths = { "customer" })
	@Query(value = """
			select v from Vehicle v
			join v.customer c
			where v.workshop.id = :workshopId
			  and (:customerId is null or c.id = :customerId)
			  and (:active is null or v.active = :active)
			  and (:term is null or :term = ''
			       or lower(v.plate) like lower(concat('%', :term, '%'))
			       or lower(v.brand) like lower(concat('%', :term, '%'))
			       or lower(v.model) like lower(concat('%', :term, '%'))
			       or lower(c.name) like lower(concat('%', :term, '%')))
			""",
			countQuery = """
			select count(v) from Vehicle v
			join v.customer c
			where v.workshop.id = :workshopId
			  and (:customerId is null or c.id = :customerId)
			  and (:active is null or v.active = :active)
			  and (:term is null or :term = ''
			       or lower(v.plate) like lower(concat('%', :term, '%'))
			       or lower(v.brand) like lower(concat('%', :term, '%'))
			       or lower(v.model) like lower(concat('%', :term, '%'))
			       or lower(c.name) like lower(concat('%', :term, '%')))
			""")
	Page<Vehicle> search(@Param("workshopId") UUID workshopId, @Param("term") String term,
			@Param("customerId") UUID customerId, @Param("active") Boolean active, Pageable pageable);
}
