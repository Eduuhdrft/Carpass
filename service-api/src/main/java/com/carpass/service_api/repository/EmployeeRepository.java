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

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.Employee;

/**
 * Funcionarios da oficina.
 */
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

	Optional<Employee> findByIdAndWorkshopId(UUID id, UUID workshopId);

	List<Employee> findByWorkshopIdAndActiveTrueOrderByNameAsc(UUID workshopId);

	List<Employee> findByWorkshopIdAndRoleAndActiveTrueOrderByNameAsc(UUID workshopId, Role role);

	boolean existsByWorkshopIdAndUserId(UUID workshopId, UUID userId);

	long countByWorkshopIdAndActiveTrue(UUID workshopId);

	@EntityGraph(attributePaths = { "user" })
	@Query(value = """
			select e from Employee e
			where e.workshop.id = :workshopId
			  and (:active is null or e.active = :active)
			  and (:role is null or e.role = :role)
			  and (:term is null or :term = ''
			       or lower(e.name) like lower(concat('%', :term, '%'))
			       or lower(coalesce(e.specialty, '')) like lower(concat('%', :term, '%'))
			       or lower(coalesce(e.email, '')) like lower(concat('%', :term, '%')))
			""",
			countQuery = """
			select count(e) from Employee e
			where e.workshop.id = :workshopId
			  and (:active is null or e.active = :active)
			  and (:role is null or e.role = :role)
			  and (:term is null or :term = ''
			       or lower(e.name) like lower(concat('%', :term, '%'))
			       or lower(coalesce(e.specialty, '')) like lower(concat('%', :term, '%'))
			       or lower(coalesce(e.email, '')) like lower(concat('%', :term, '%')))
			""")
	Page<Employee> search(@Param("workshopId") UUID workshopId, @Param("term") String term,
			@Param("role") Role role, @Param("active") Boolean active, Pageable pageable);
}
