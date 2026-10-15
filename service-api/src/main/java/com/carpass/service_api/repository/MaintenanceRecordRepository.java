package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.MaintenanceRecord;

/**
 * Historico de manutencao dos veiculos.
 */
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

	Optional<MaintenanceRecord> findByIdAndWorkshopId(UUID id, UUID workshopId);

	List<MaintenanceRecord> findByWorkshopIdAndVehicleIdOrderByServiceDateDesc(UUID workshopId, UUID vehicleId);

	Optional<MaintenanceRecord> findByWorkOrderId(UUID workOrderId);
}
