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
import com.carpass.service_api.service.PartService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.part.PartFilter;
import com.carpass.service_api.web.dto.part.PartManufacturerResponse;
import com.carpass.service_api.web.dto.part.PartRequest;
import com.carpass.service_api.web.dto.part.PartResponse;
import com.carpass.service_api.web.dto.part.StockMovementRequest;
import com.carpass.service_api.web.dto.part.StockMovementResponse;

import jakarta.validation.Valid;

/**
 * Pecas e estoque.
 */
@RestController
@RequestMapping("/api/v1/parts")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class PartController {

	private final PartService partService;

	public PartController(PartService partService) {
		this.partService = partService;
	}

	/**
	 * Lista paginada com busca por nome, codigo ou fabricante.
	 *
	 * @param lowStock quando verdadeiro, traz apenas o que esta no estoque minimo
	 *                 ou abaixo dele
	 * @param typeId   tipo de peca (oleo de motor, vela, bobina...)
	 * @param brand    marca, escolhida da lista devolvida por {@code /manufacturers}
	 */
	@GetMapping
	public ResponseEntity<PageResponse<PartResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "false") boolean lowStock,
			@RequestParam(required = false) UUID typeId,
			@RequestParam(required = false) String brand,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(partService.search(new PartFilter(q, active, lowStock, typeId, brand), pageable));
	}

	/**
	 * Marcas presentes no estoque, opcionalmente de um tipo so.
	 *
	 * <p>
	 * Alimenta o filtro de marca. Sem isto a tela so poderia oferecer as marcas da
	 * pagina que carregou -- e filtro que esconde opcao valida e pior que filtro
	 * nenhum.
	 * </p>
	 */
	@GetMapping("/manufacturers")
	public ResponseEntity<List<PartManufacturerResponse>> manufacturers(
			@RequestParam(required = false) UUID typeId) {
		return ResponseEntity.ok(partService.findManufacturers(typeId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PartResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(partService.findById(id));
	}

	/** Historico de movimentacao de estoque da peca. */
	@GetMapping("/{id}/stock-movements")
	public ResponseEntity<PageResponse<StockMovementResponse>> stockMovements(@PathVariable UUID id,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(partService.findStockMovements(id, pageable));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartResponse> create(@RequestBody @Valid PartRequest request) {
		PartResponse created = partService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/parts/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartResponse> update(@PathVariable UUID id, @RequestBody @Valid PartRequest request) {
		return ResponseEntity.ok(partService.update(id, request));
	}

	/** Entrada, saida ou ajuste manual de estoque. */
	@PostMapping("/{id}/stock-movements")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartResponse> registerStockMovement(@PathVariable UUID id,
			@RequestBody @Valid StockMovementRequest request) {
		return ResponseEntity.ok(partService.registerStockMovement(id, request));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(partService.changeStatus(id, request.active()));
	}

	/** Desativa a peca, preservando o historico de movimentacao e de uso em OS. */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<PartResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(partService.changeStatus(id, false));
	}
}
