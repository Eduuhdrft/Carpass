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

import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.CustomerService;
import com.carpass.service_api.service.VehicleService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.customer.CustomerRequest;
import com.carpass.service_api.web.dto.customer.CustomerResponse;
import com.carpass.service_api.web.dto.vehicle.VehicleResponse;

import jakarta.validation.Valid;

/**
 * Clientes da oficina.
 */
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class CustomerController {

	private final CustomerService customerService;
	private final VehicleService vehicleService;

	public CustomerController(CustomerService customerService, VehicleService vehicleService) {
		this.customerService = customerService;
		this.vehicleService = vehicleService;
	}

	/**
	 * Lista paginada com busca por nome, documento, telefone ou e-mail.
	 *
	 * @param q      termo de busca livre
	 * @param active filtra por ativos ou inativos; nulo traz os dois
	 */
	@GetMapping
	public ResponseEntity<PageResponse<CustomerResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(customerService.search(q, active, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(customerService.findById(id));
	}

	/** Veiculos do cliente. */
	@GetMapping("/{id}/vehicles")
	public ResponseEntity<List<VehicleResponse>> vehicles(@PathVariable UUID id) {
		return ResponseEntity.ok(vehicleService.findByCustomer(id));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<CustomerResponse> create(@RequestBody @Valid CustomerRequest request) {
		CustomerResponse created = customerService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<CustomerResponse> update(@PathVariable UUID id, @RequestBody @Valid CustomerRequest request) {
		return ResponseEntity.ok(customerService.update(id, request));
	}

	/** Reativa ou desativa o cliente. */
	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<CustomerResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(customerService.changeStatus(id, request.active()));
	}

	/**
	 * Desativa o cliente. Nao ha exclusao fisica: apagar o cadastro quebraria o
	 * historico das ordens de servico ja emitidas.
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<CustomerResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(customerService.changeStatus(id, false));
	}
}
