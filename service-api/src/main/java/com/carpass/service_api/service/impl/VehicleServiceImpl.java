package com.carpass.service_api.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.CustomerRepository;
import com.carpass.service_api.repository.VehicleRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.VehicleService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.vehicle.VehicleRequest;
import com.carpass.service_api.web.dto.vehicle.VehicleResponse;

/**
 * Regras de cadastro de veiculos.
 *
 * <p>
 * O cliente proprietario tambem e carregado com filtro por oficina: sem isso,
 * seria possivel vincular um veiculo a um cliente de outra oficina apenas
 * enviando o id dele no corpo da requisicao.
 * </p>
 */
@Service
public class VehicleServiceImpl implements VehicleService {

	private static final Logger log = LoggerFactory.getLogger(VehicleServiceImpl.class);

	/** Fabricantes lancam o modelo do ano seguinte, entao aceitamos um ano a frente. */
	private static final int MAX_YEARS_AHEAD = 1;
	private static final int MIN_VEHICLE_YEAR = 1900;

	private final VehicleRepository vehicleRepository;
	private final CustomerRepository customerRepository;
	private final WorkshopRepository workshopRepository;
	private final CurrentUser currentUser;

	public VehicleServiceImpl(VehicleRepository vehicleRepository, CustomerRepository customerRepository,
			WorkshopRepository workshopRepository, CurrentUser currentUser) {
		this.vehicleRepository = vehicleRepository;
		this.customerRepository = customerRepository;
		this.workshopRepository = workshopRepository;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<VehicleResponse> search(String term, UUID customerId, Boolean active, Pageable pageable) {
		return PageResponse.from(vehicleRepository.search(currentUser.requireWorkshopId(), normalize(term), customerId,
				active, pageable), VehicleResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleResponse> findByCustomer(UUID customerId) {
		UUID workshopId = currentUser.requireWorkshopId();
		// Confere que o cliente e desta oficina antes de listar qualquer coisa.
		customerRepository.findByIdAndWorkshopId(customerId, workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Cliente"));

		return vehicleRepository.findByWorkshopIdAndCustomerIdOrderByPlateAsc(workshopId, customerId).stream()
				.map(VehicleResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public VehicleResponse findById(UUID id) {
		return VehicleResponse.from(load(id));
	}

	@Override
	@Transactional
	public VehicleResponse create(VehicleRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		String plate = normalizePlate(request.plate());

		if (vehicleRepository.existsByWorkshopIdAndPlateIgnoreCase(workshopId, plate)) {
			throw new ConflictException("Ja existe um veiculo com esta placa nesta oficina.");
		}

		Customer customer = loadCustomer(request.customerId(), workshopId);
		Workshop workshop = workshopRepository.getReferenceById(workshopId);

		Vehicle vehicle = new Vehicle();
		vehicle.setWorkshop(workshop);
		vehicle.setActive(true);
		apply(request, vehicle, plate, customer);
		vehicleRepository.save(vehicle);

		log.info("Veiculo criado: veiculo={} cliente={} oficina={}", vehicle.getId(), customer.getId(), workshopId);
		return VehicleResponse.from(vehicle);
	}

	@Override
	@Transactional
	public VehicleResponse update(UUID id, VehicleRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		Vehicle vehicle = load(id);
		String plate = normalizePlate(request.plate());

		if (vehicleRepository.existsByWorkshopIdAndPlateIgnoreCaseAndIdNot(workshopId, plate, id)) {
			throw new ConflictException("Ja existe outro veiculo com esta placa nesta oficina.");
		}

		Customer customer = loadCustomer(request.customerId(), workshopId);
		apply(request, vehicle, plate, customer);
		vehicleRepository.save(vehicle);

		log.info("Veiculo atualizado: veiculo={}", id);
		return VehicleResponse.from(vehicle);
	}

	@Override
	@Transactional
	public VehicleResponse changeStatus(UUID id, boolean active) {
		Vehicle vehicle = load(id);
		vehicle.setActive(active);
		vehicleRepository.save(vehicle);

		log.info("Veiculo {}: veiculo={}", active ? "reativado" : "desativado", id);
		return VehicleResponse.from(vehicle);
	}

	// -------------------------------------------------------------- apoio
	private void apply(VehicleRequest request, Vehicle vehicle, String plate, Customer customer) {
		validateYear(request.manufactureYear(), "ano de fabricacao");
		validateYear(request.modelYear(), "ano do modelo");

		vehicle.setCustomer(customer);
		vehicle.setPlate(plate);
		vehicle.setBrand(request.brand().trim());
		vehicle.setModel(request.model().trim());
		vehicle.setManufactureYear(request.manufactureYear());
		vehicle.setModelYear(request.modelYear());
		vehicle.setMileage(request.mileage());
		vehicle.setFuelType(request.fuelType());
		// Tipo e codigo FIPE registram a origem do cadastro. Ficam vazios quando a
		// pessoa digitou marca e modelo a mao, e isso e um estado legitimo.
		vehicle.setVehicleKind(request.vehicleKind());
		vehicle.setFipeCode(blankToNull(request.fipeCode()));
		vehicle.setChassis(blankToNull(request.chassis()));
		vehicle.setRenavam(blankToNull(request.renavam()));
		vehicle.setColor(blankToNull(request.color()));
		vehicle.setNotes(request.notes());
	}

	private void validateYear(Integer year, String label) {
		if (year == null) {
			return;
		}
		int limit = LocalDate.now().getYear() + MAX_YEARS_AHEAD;
		if (year < MIN_VEHICLE_YEAR || year > limit) {
			throw new BusinessException("O " + label + " deve estar entre " + MIN_VEHICLE_YEAR + " e " + limit + ".");
		}
	}

	private Customer loadCustomer(UUID customerId, UUID workshopId) {
		return customerRepository.findByIdAndWorkshopId(customerId, workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Cliente"));
	}

	private Vehicle load(UUID id) {
		return vehicleRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Veiculo"));
	}

	/** Placa e sempre guardada em caixa alta e sem separadores. */
	private String normalizePlate(String plate) {
		return plate.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}
}
