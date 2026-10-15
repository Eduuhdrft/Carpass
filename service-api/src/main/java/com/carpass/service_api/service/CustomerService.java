package com.carpass.service_api.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.customer.CustomerRequest;
import com.carpass.service_api.web.dto.customer.CustomerResponse;

/**
 * Clientes da oficina.
 */
public interface CustomerService {

	PageResponse<CustomerResponse> search(String term, Boolean active, Pageable pageable);

	CustomerResponse findById(UUID id);

	CustomerResponse create(CustomerRequest request);

	CustomerResponse update(UUID id, CustomerRequest request);

	/** Ativa ou desativa o cliente. Nao ha exclusao fisica. */
	CustomerResponse changeStatus(UUID id, boolean active);
}
