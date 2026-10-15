package com.carpass.service_api.web.controller;

import java.net.URI;
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
import com.carpass.service_api.service.ServiceCatalogService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogRequest;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogResponse;

import jakarta.validation.Valid;

/**
 * Catalogo de servicos oferecidos pela oficina.
 */
@RestController
@RequestMapping("/api/v1/services")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class ServiceCatalogController {

	private final ServiceCatalogService serviceCatalogService;

	public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
		this.serviceCatalogService = serviceCatalogService;
	}

	@GetMapping
	public ResponseEntity<PageResponse<ServiceCatalogResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(serviceCatalogService.search(q, active, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ServiceCatalogResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(serviceCatalogService.findById(id));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<ServiceCatalogResponse> create(@RequestBody @Valid ServiceCatalogRequest request) {
		ServiceCatalogResponse created = serviceCatalogService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/services/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<ServiceCatalogResponse> update(@PathVariable UUID id,
			@RequestBody @Valid ServiceCatalogRequest request) {
		return ResponseEntity.ok(serviceCatalogService.update(id, request));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<ServiceCatalogResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(serviceCatalogService.changeStatus(id, request.active()));
	}

	/** Desativa o servico. Ordens antigas que o usaram continuam intactas. */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<ServiceCatalogResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(serviceCatalogService.changeStatus(id, false));
	}
}
