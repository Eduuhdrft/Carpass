package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.vehicle.VehicleRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleResponse;

/**
 * Veiculos atendidos pela oficina.
 */
public interface VehicleService {

	PageResponse<VehicleResponse> search(String term, UUID customerId, Boolean active, Pageable pageable);

	List<VehicleResponse> findByCustomer(UUID customerId);

	VehicleResponse findById(UUID id);

	VehicleResponse create(VehicleRequest request);

	VehicleResponse update(UUID id, VehicleRequest request);

	VehicleResponse changeStatus(UUID id, boolean active);
}
