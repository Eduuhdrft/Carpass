package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import com.carpass.service_api.web.dto.vehicle.MaintenanceRecordRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleHistoryEntryResponse;

/**
 * Linha do tempo de manutencao de um veiculo.
 */
public interface VehicleHistoryService {

	/** Historico completo, do mais recente para o mais antigo. */
	List<VehicleHistoryEntryResponse> findByVehicle(UUID vehicleId);

	/** Registra um servico feito fora desta oficina. */
	VehicleHistoryEntryResponse addManualRecord(UUID vehicleId, MaintenanceRecordRequest request);

	void removeManualRecord(UUID vehicleId, UUID recordId);
}
