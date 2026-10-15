package com.carpass.service_api.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.ItemType;
import com.carpass.service_api.domain.model.MaintenanceRecord;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.domain.model.WorkOrderItem;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.MaintenanceRecordRepository;
import com.carpass.service_api.repository.VehicleRepository;
import com.carpass.service_api.repository.WorkOrderRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.VehicleHistoryService;
import com.carpass.service_api.web.dto.vehicle.MaintenanceRecordRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleHistoryEntryResponse;

/**
 * Historico de manutencao do veiculo.
 *
 * <p>
 * A linha do tempo e montada na leitura, a partir de duas fontes: as ordens de
 * servico concluidas nesta oficina e os lancamentos manuais de servicos feitos
 * em outro lugar.
 * </p>
 *
 * <p>
 * Optamos por nao copiar a OS entregue para uma tabela de historico. Duplicar o
 * dado significaria manter duas verdades em sincronia, e qualquer correcao feita
 * em uma OS deixaria o historico desatualizado. Aqui a ordem de servico continua
 * sendo a unica fonte do que a oficina executou.
 * </p>
 */
@Service
public class VehicleHistoryServiceImpl implements VehicleHistoryService {

	private static final Logger log = LoggerFactory.getLogger(VehicleHistoryServiceImpl.class);

	private static final String SOURCE_WORK_ORDER = "WORK_ORDER";
	private static final String SOURCE_MANUAL = "MANUAL";

	private final VehicleRepository vehicleRepository;
	private final WorkOrderRepository workOrderRepository;
	private final MaintenanceRecordRepository maintenanceRecordRepository;
	private final WorkshopRepository workshopRepository;
	private final CurrentUser currentUser;

	public VehicleHistoryServiceImpl(VehicleRepository vehicleRepository, WorkOrderRepository workOrderRepository,
			MaintenanceRecordRepository maintenanceRecordRepository, WorkshopRepository workshopRepository,
			CurrentUser currentUser) {
		this.vehicleRepository = vehicleRepository;
		this.workOrderRepository = workOrderRepository;
		this.maintenanceRecordRepository = maintenanceRecordRepository;
		this.workshopRepository = workshopRepository;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleHistoryEntryResponse> findByVehicle(UUID vehicleId) {
		UUID workshopId = currentUser.requireWorkshopId();
		loadVehicle(vehicleId, workshopId);

		List<VehicleHistoryEntryResponse> timeline = new ArrayList<>();

		for (WorkOrder order : workOrderRepository.findCompletedByVehicle(workshopId, vehicleId)) {
			timeline.add(fromWorkOrder(order));
		}
		for (MaintenanceRecord record : maintenanceRecordRepository
				.findByWorkshopIdAndVehicleIdOrderByServiceDateDesc(workshopId, vehicleId)) {
			timeline.add(fromManualRecord(record));
		}

		// Mais recente primeiro; entradas sem data ficam por ultimo.
		timeline.sort(Comparator.comparing(VehicleHistoryEntryResponse::date,
				Comparator.nullsLast(Comparator.reverseOrder())));

		return timeline;
	}

	@Override
	@Transactional
	public VehicleHistoryEntryResponse addManualRecord(UUID vehicleId, MaintenanceRecordRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		Vehicle vehicle = loadVehicle(vehicleId, workshopId);

		MaintenanceRecord record = new MaintenanceRecord();
		record.setWorkshop(workshopRepository.getReferenceById(workshopId));
		record.setVehicle(vehicle);
		record.setServiceDate(request.serviceDate());
		record.setMileage(request.mileage());
		record.setDescription(request.description().trim());
		record.setPerformedBy(request.performedBy());
		record.setAmount(request.amount());
		record.setNotes(request.notes());
		maintenanceRecordRepository.save(record);

		log.info("Historico manual adicionado: veiculo={} registro={}", vehicleId, record.getId());
		return fromManualRecord(record);
	}

	@Override
	@Transactional
	public void removeManualRecord(UUID vehicleId, UUID recordId) {
		UUID workshopId = currentUser.requireWorkshopId();
		loadVehicle(vehicleId, workshopId);

		MaintenanceRecord record = maintenanceRecordRepository.findByIdAndWorkshopId(recordId, workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Registro de historico"));

		maintenanceRecordRepository.delete(record);
		log.info("Historico manual removido: veiculo={} registro={}", vehicleId, recordId);
	}

	// -------------------------------------------------------------- apoio
	private VehicleHistoryEntryResponse fromWorkOrder(WorkOrder order) {
		List<String> services = new ArrayList<>();
		List<String> parts = new ArrayList<>();

		for (WorkOrderItem item : order.getItems()) {
			String label = item.getQuantity() > 1 ? item.getQuantity() + "x " + item.getDescription()
					: item.getDescription();
			if (item.getType() == ItemType.PART) {
				parts.add(label);
			} else {
				services.add(label);
			}
		}

		return new VehicleHistoryEntryResponse(SOURCE_WORK_ORDER, order.getId(), order.getId(), order.getNumber(),
				historyDateOf(order), order.getMileage(), describe(order, services), services, parts,
				order.getAssignedMechanic() == null ? null : order.getAssignedMechanic().getName(),
				order.getTotalAmount(), order.getNotes());
	}

	private VehicleHistoryEntryResponse fromManualRecord(MaintenanceRecord record) {
		return new VehicleHistoryEntryResponse(SOURCE_MANUAL, record.getId(), null, null, record.getServiceDate(),
				record.getMileage(), record.getDescription(), List.of(), List.of(), record.getPerformedBy(),
				record.getAmount(), record.getNotes());
	}

	/** Data que representa o evento: entrega, conclusao ou, na falta das duas, abertura. */
	private java.time.LocalDate historyDateOf(WorkOrder order) {
		if (order.getDeliveredAt() != null) {
			return order.getDeliveredAt().toLocalDate();
		}
		if (order.getFinishedAt() != null) {
			return order.getFinishedAt().toLocalDate();
		}
		return order.getOpenedAt() == null ? null : order.getOpenedAt().toLocalDate();
	}

	private String describe(WorkOrder order, List<String> services) {
		if (!services.isEmpty()) {
			return String.join(", ", services);
		}
		return "Ordem de servico #" + order.getNumber();
	}

	private Vehicle loadVehicle(UUID vehicleId, UUID workshopId) {
		return vehicleRepository.findByIdAndWorkshopId(vehicleId, workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Veiculo"));
	}
}
