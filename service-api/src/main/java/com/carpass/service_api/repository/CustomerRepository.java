package com.carpass.service_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.Customer;

/**
 * Clientes da oficina. Toda consulta e obrigatoriamente filtrada por oficina.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	Optional<Customer> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndDocumentIgnoreCase(UUID workshopId, String document);

	boolean existsByWorkshopIdAndDocumentIgnoreCaseAndIdNot(UUID workshopId, String document, UUID id);

	long countByWorkshopIdAndActiveTrue(UUID workshopId);

	/**
	 * Busca por nome, documento, telefone ou e-mail. O termo nulo ou vazio devolve
	 * todos os clientes da oficina.
	 */
	@Query("""
			select c from Customer c
			where c.workshop.id = :workshopId
			  and (:active is null or c.active = :active)
			  and (:term is null or :term = ''
			       or lower(c.name) like lower(concat('%', :term, '%'))
			       or lower(coalesce(c.document, '')) like lower(concat('%', :term, '%'))
			       or lower(coalesce(c.phone, '')) like lower(concat('%', :term, '%'))
			       or lower(coalesce(c.email, '')) like lower(concat('%', :term, '%')))
			""")
	Page<Customer> search(@Param("workshopId") UUID workshopId, @Param("term") String term,
			@Param("active") Boolean active, Pageable pageable);
}
