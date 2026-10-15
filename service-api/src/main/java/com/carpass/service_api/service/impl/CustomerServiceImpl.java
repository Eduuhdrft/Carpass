package com.carpass.service_api.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.CustomerRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.CustomerService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.customer.CustomerRequest;
import com.carpass.service_api.web.dto.customer.CustomerResponse;

/**
 * Regras de cadastro de clientes.
 *
 * <p>
 * Toda leitura e escrita passa por {@code workshopId} vindo do usuario
 * autenticado. Um cliente de outra oficina simplesmente nao existe do ponto de
 * vista desta sessao, e a resposta e 404.
 * </p>
 */
@Service
public class CustomerServiceImpl implements CustomerService {

	private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

	private final CustomerRepository customerRepository;
	private final WorkshopRepository workshopRepository;
	private final CurrentUser currentUser;

	public CustomerServiceImpl(CustomerRepository customerRepository, WorkshopRepository workshopRepository,
			CurrentUser currentUser) {
		this.customerRepository = customerRepository;
		this.workshopRepository = workshopRepository;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CustomerResponse> search(String term, Boolean active, Pageable pageable) {
		return PageResponse.from(
				customerRepository.search(currentUser.requireWorkshopId(), normalize(term), active, pageable),
				CustomerResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponse findById(UUID id) {
		return CustomerResponse.from(load(id));
	}

	@Override
	@Transactional
	public CustomerResponse create(CustomerRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		String document = onlyDigits(request.document());

		if (document != null && customerRepository.existsByWorkshopIdAndDocumentIgnoreCase(workshopId, document)) {
			throw new ConflictException("Ja existe um cliente com este CPF/CNPJ nesta oficina.");
		}

		Workshop workshop = workshopRepository.getReferenceById(workshopId);

		Customer customer = new Customer();
		customer.setWorkshop(workshop);
		customer.setActive(true);
		apply(request, customer, document);
		customerRepository.save(customer);

		log.info("Cliente criado: cliente={} oficina={}", customer.getId(), workshopId);
		return CustomerResponse.from(customer);
	}

	@Override
	@Transactional
	public CustomerResponse update(UUID id, CustomerRequest request) {
		Customer customer = load(id);
		String document = onlyDigits(request.document());

		if (document != null && customerRepository.existsByWorkshopIdAndDocumentIgnoreCaseAndIdNot(
				currentUser.requireWorkshopId(), document, id)) {
			throw new ConflictException("Ja existe outro cliente com este CPF/CNPJ nesta oficina.");
		}

		apply(request, customer, document);
		customerRepository.save(customer);

		log.info("Cliente atualizado: cliente={}", id);
		return CustomerResponse.from(customer);
	}

	@Override
	@Transactional
	public CustomerResponse changeStatus(UUID id, boolean active) {
		Customer customer = load(id);
		customer.setActive(active);
		customerRepository.save(customer);

		log.info("Cliente {}: cliente={}", active ? "reativado" : "desativado", id);
		return CustomerResponse.from(customer);
	}

	// -------------------------------------------------------------- apoio
	private void apply(CustomerRequest request, Customer customer, String document) {
		customer.setType(request.type());
		customer.setName(request.name().trim());
		customer.setDocument(document);
		customer.setPhone(request.phone());
		customer.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
		customer.setAddress(request.address() == null ? null : request.address().toAddress());
		customer.setNotes(request.notes());
	}

	private Customer load(UUID id) {
		return customerRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Cliente"));
	}

	private String onlyDigits(String value) {
		if (value == null) {
			return null;
		}
		String digits = value.replaceAll("\\D", "");
		return digits.isEmpty() ? null : digits;
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}
}
