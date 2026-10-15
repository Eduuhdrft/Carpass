package com.carpass.service_api.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.EmployeeService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.employee.EmployeeRequest;
import com.carpass.service_api.web.dto.employee.EmployeeResponse;
import com.carpass.service_api.web.dto.employee.GrantAccessRequest;

import jakarta.validation.Valid;

/**
 * Equipe da oficina.
 *
 * <p>
 * Consultar a equipe e liberado para qualquer papel, porque a ordem de servico
 * precisa listar mecanicos. Criar, editar e conceder acesso ficam restritos a
 * proprietario e gerente.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/employees")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping
	public ResponseEntity<PageResponse<EmployeeResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(employeeService.search(q, role, active, pageable));
	}

	/** Mecanicos ativos, para o seletor de responsavel da ordem de servico. */
	@GetMapping("/mechanics")
	public ResponseEntity<List<EmployeeResponse>> mechanics() {
		return ResponseEntity.ok(employeeService.findActiveMechanics());
	}

	@GetMapping("/{id}")
	public ResponseEntity<EmployeeResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(employeeService.findById(id));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<EmployeeResponse> create(@RequestBody @Valid EmployeeRequest request) {
		EmployeeResponse created = employeeService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/employees/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<EmployeeResponse> update(@PathVariable UUID id, @RequestBody @Valid EmployeeRequest request) {
		return ResponseEntity.ok(employeeService.update(id, request));
	}

	/**
	 * Cria a conta de acesso do funcionario e dispara o e-mail de primeiro acesso.
	 * A senha e definida pelo proprio funcionario, nunca por quem cadastra.
	 */
	@PostMapping("/{id}/access")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<EmployeeResponse> grantAccess(@PathVariable UUID id,
			@RequestBody @Valid GrantAccessRequest request) {
		return ResponseEntity.ok(employeeService.grantSystemAccess(id, request));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<EmployeeResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(employeeService.changeStatus(id, request.active()));
	}

	/** Desativa o funcionario e, junto, o acesso dele ao sistema. */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<EmployeeResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(employeeService.changeStatus(id, false));
	}
}
