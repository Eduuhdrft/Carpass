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
import com.carpass.service_api.service.VehicleHistoryService;
import com.carpass.service_api.service.VehicleService;
import com.carpass.service_api.web.dto.common.ActiveStatusRequest;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.vehicle.MaintenanceRecordRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleHistoryEntryResponse;
import com.carpass.service_api.web.dto.vehicle.VehicleRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleResponse;

import jakarta.validation.Valid;

/**
 * Veiculos atendidos pela oficina.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class VehicleController {

	private final VehicleService vehicleService;
	private final VehicleHistoryService vehicleHistoryService;

	public VehicleController(VehicleService vehicleService, VehicleHistoryService vehicleHistoryService) {
		this.vehicleService = vehicleService;
		this.vehicleHistoryService = vehicleHistoryService;
	}

	/**
	 * Lista paginada com busca por placa, marca, modelo ou nome do cliente.
	 *
	 * @param customerId restringe a um cliente especifico
	 */
	@GetMapping
	public ResponseEntity<PageResponse<VehicleResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "plate", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(vehicleService.search(q, customerId, active, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<VehicleResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(vehicleService.findById(id));
	}

	/**
	 * Linha do tempo de manutencao do veiculo.
	 *
	 * <p>
	 * Junta as ordens de servico concluidas nesta oficina com os lancamentos
	 * manuais de servicos feitos em outro lugar, do mais recente para o mais
	 * antigo.
	 * </p>
	 */
	@GetMapping("/{id}/history")
	public ResponseEntity<List<VehicleHistoryEntryResponse>> history(@PathVariable UUID id) {
		return ResponseEntity.ok(vehicleHistoryService.findByVehicle(id));
	}

	/** Registra no historico um servico feito fora desta oficina. */
	@PostMapping("/{id}/history")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<VehicleHistoryEntryResponse> addHistoryRecord(@PathVariable UUID id,
			@RequestBody @Valid MaintenanceRecordRequest request) {
		return ResponseEntity.ok(vehicleHistoryService.addManualRecord(id, request));
	}

	/** Remove um lancamento manual. Ordens de servico nao sao apagadas por aqui. */
	@DeleteMapping("/{id}/history/{recordId}")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<Void> removeHistoryRecord(@PathVariable UUID id, @PathVariable UUID recordId) {
		vehicleHistoryService.removeManualRecord(id, recordId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<VehicleResponse> create(@RequestBody @Valid VehicleRequest request) {
		VehicleResponse created = vehicleService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/vehicles/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<VehicleResponse> update(@PathVariable UUID id, @RequestBody @Valid VehicleRequest request) {
		return ResponseEntity.ok(vehicleService.update(id, request));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<VehicleResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid ActiveStatusRequest request) {
		return ResponseEntity.ok(vehicleService.changeStatus(id, request.active()));
	}

	/** Desativa o veiculo, preservando o historico de manutencao. */
	@DeleteMapping("/{id}")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<VehicleResponse> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(vehicleService.changeStatus(id, false));
	}
}
