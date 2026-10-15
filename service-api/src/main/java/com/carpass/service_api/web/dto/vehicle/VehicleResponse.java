package com.carpass.service_api.web.dto.vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.FuelType;
import com.carpass.service_api.domain.enums.VehicleKind;
import com.carpass.service_api.domain.model.Vehicle;

/**
 * Veiculo devolvido pela API, ja com o nome do cliente para evitar uma segunda
 * chamada so para montar a listagem.
 */
public record VehicleResponse(UUID id, UUID customerId, String customerName, String plate, String brand, String model,
		Integer manufactureYear, Integer modelYear, Integer mileage, FuelType fuelType, String fuelTypeLabel,
		VehicleKind vehicleKind, String fipeCode, String chassis, String renavam, String color, String notes,
		boolean active, LocalDateTime createdAt) {

	public static VehicleResponse from(Vehicle vehicle) {
		return new VehicleResponse(vehicle.getId(), vehicle.getCustomer().getId(), vehicle.getCustomer().getName(),
				vehicle.getPlate(), vehicle.getBrand(), vehicle.getModel(), vehicle.getManufactureYear(),
				vehicle.getModelYear(), vehicle.getMileage(), vehicle.getFuelType(),
				vehicle.getFuelType() == null ? null : vehicle.getFuelType().getLabel(), vehicle.getVehicleKind(),
				vehicle.getFipeCode(), vehicle.getChassis(), vehicle.getRenavam(), vehicle.getColor(),
				vehicle.getNotes(), vehicle.isActive(), vehicle.getCreatedAt());
	}
}
