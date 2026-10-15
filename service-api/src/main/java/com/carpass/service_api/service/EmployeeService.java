package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.employee.EmployeeRequest;
import com.carpass.service_api.web.dto.employee.EmployeeResponse;
import com.carpass.service_api.web.dto.employee.GrantAccessRequest;

/**
 * Equipe da oficina.
 */
public interface EmployeeService {

	PageResponse<EmployeeResponse> search(String term, Role role, Boolean active, Pageable pageable);

	/** Mecanicos ativos, para preencher o seletor de responsavel na ordem de servico. */
	List<EmployeeResponse> findActiveMechanics();

	EmployeeResponse findById(UUID id);

	EmployeeResponse create(EmployeeRequest request);

	EmployeeResponse update(UUID id, EmployeeRequest request);

	EmployeeResponse changeStatus(UUID id, boolean active);

	/**
	 * Cria a conta de acesso do funcionario e envia o link de primeiro acesso.
	 */
	EmployeeResponse grantSystemAccess(UUID id, GrantAccessRequest request);
}
