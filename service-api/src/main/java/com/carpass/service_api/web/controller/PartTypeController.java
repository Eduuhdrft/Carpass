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
import com.carpass.service_api.service.PartTypeService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.parttype.PartTypeRequest;
import com.carpass.service_api.web.dto.parttype.PartTypeResponse;

import jakarta.validation.Valid;

/**
 * Tipos de peca.
 *
 * <p>
 * Qualquer pessoa da equipe le -- o mecanico precisa filtrar o estoque tanto
 * quanto o dono. Mexer no cadastro e decisao administrativa, como o catalogo de
 * servicos e as proprias pecas.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/part-types")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class PartTypeController {

	private final PartTypeService partTypeService;

	public PartTypeController(PartTypeService partTypeService) {
		this.partTypeService = partTypeService;
	}

	@GetMapping
	public ResponseEntity<PageResponse<PartTypeResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(partTypeService.search(q, active, pageable));
	}

	/**
	 * Lista completa dos tipos ativos, sem paginacao.
	 *
	 * <p>
	 * O seletor do formulario e o filtro do estoque precisam de todos de uma vez:
	 * paginar uma lista de escolha esconderia opcao valida atras de um "proxima
	 * pagina" que ninguem espera num campo de selecao.
	 * </p>
	 */
	@GetMapping("/active")
	public ResponseEntity<List<PartTypeResponse>> listActive() {
		return ResponseEntity.ok(partTypeService.listActive());
	}

	@GetMapping("/{id}")
	public ResponseEntity<PartTypeResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(partTypeService.findById(id));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartTypeResponse> create(@RequestBody @Valid PartTypeRequest request) {
		PartTypeResponse created = partTypeService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/part-types/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartTypeResponse> update(@PathVariable UUID id,
			@RequestBody @Valid PartTypeRequest request) {
		return ResponseEntity.ok(partTypeService.update(id, request));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartTypeResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(partTypeService.changeStatus(id, request.active()));
	}

	/** Desativa o tipo. As pecas que o usam continuam intactas. */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartTypeResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(partTypeService.changeStatus(id, false));
	}
}
