package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.enums.CustomerType;
import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.domain.model.Workshop;

/**
 * Isolamento entre oficinas.
 *
 * <p>
 * Esta e a garantia mais importante do sistema: os dados de uma oficina nao
 * podem aparecer para outra em hipotese nenhuma. Os testes abaixo montam duas
 * oficinas completas e verificam que as consultas com escopo por oficina nunca
 * atravessam essa fronteira, nem mesmo quando o identificador do registro alheio
 * e conhecido.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class TenantIsolationTest {

	@Autowired
	private WorkshopRepository workshopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private WorkOrderRepository workOrderRepository;

	private Workshop workshopA;
	private Workshop workshopB;
	private Customer customerA;
	private Customer customerB;
	private Vehicle vehicleA;
	private WorkOrder orderA;

	@BeforeEach
	void setUp() {
		workshopA = createWorkshop("Oficina A", "11111111000191", "dono.a@teste.com");
		workshopB = createWorkshop("Oficina B", "22222222000192", "dono.b@teste.com");

		customerA = createCustomer(workshopA, "Cliente da oficina A", "11122233344");
		customerB = createCustomer(workshopB, "Cliente da oficina B", "55566677788");

		vehicleA = createVehicle(workshopA, customerA, "ABC1D23");
		createVehicle(workshopB, customerB, "XYZ9K88");

		orderA = createWorkOrder(workshopA, customerA, vehicleA);
	}

	@Test
	@DisplayName("buscar cliente de outra oficina pelo id nao retorna nada")
	void doesNotFindCustomerFromAnotherWorkshop() {
		assertThat(customerRepository.findByIdAndWorkshopId(customerB.getId(), workshopA.getId())).isEmpty();
		assertThat(customerRepository.findByIdAndWorkshopId(customerA.getId(), workshopA.getId())).isPresent();
	}

	@Test
	@DisplayName("buscar veiculo de outra oficina pelo id nao retorna nada")
	void doesNotFindVehicleFromAnotherWorkshop() {
		assertThat(vehicleRepository.findByIdAndWorkshopId(vehicleA.getId(), workshopB.getId())).isEmpty();
		assertThat(vehicleRepository.findByIdAndWorkshopId(vehicleA.getId(), workshopA.getId())).isPresent();
	}

	@Test
	@DisplayName("a listagem de clientes so devolve os da propria oficina")
	void customerSearchIsScopedToWorkshop() {
		var page = customerRepository.search(workshopA.getId(), null, null, PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).singleElement()
				.satisfies(customer -> assertThat(customer.getName()).isEqualTo("Cliente da oficina A"));
	}

	@Test
	@DisplayName("buscar por um termo que casa com dados alheios nao vaza resultados")
	void customerSearchTermDoesNotLeakAcrossWorkshops() {
		var page = customerRepository.search(workshopA.getId(), "oficina B", null, PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isZero();
	}

	@Test
	@DisplayName("a listagem de veiculos so devolve os da propria oficina")
	void vehicleSearchIsScopedToWorkshop() {
		var page = vehicleRepository.search(workshopB.getId(), null, null, null, PageRequest.of(0, 20));

		assertThat(page.getContent()).singleElement()
				.satisfies(vehicle -> assertThat(vehicle.getPlate()).isEqualTo("XYZ9K88"));
	}

	@Test
	@DisplayName("a mesma placa pode existir em oficinas diferentes")
	void plateIsUniquePerWorkshopOnly() {
		assertThat(vehicleRepository.existsByWorkshopIdAndPlateIgnoreCase(workshopA.getId(), "ABC1D23")).isTrue();
		assertThat(vehicleRepository.existsByWorkshopIdAndPlateIgnoreCase(workshopB.getId(), "ABC1D23")).isFalse();
	}

	@Test
	@DisplayName("abrir a OS de outra oficina pelo id nao retorna nada")
	void doesNotFindWorkOrderFromAnotherWorkshop() {
		// Este e o ataque mais obvio contra um sistema multi-oficina: pegar um id
		// valido e trocar na URL. O escopo tem que barrar antes de qualquer regra
		// de negocio, e o servico transforma o vazio em 404 -- 403 confirmaria
		// que o registro existe.
		assertThat(workOrderRepository.findByIdAndWorkshopId(orderA.getId(), workshopB.getId())).isEmpty();
		assertThat(workOrderRepository.findByIdAndWorkshopId(orderA.getId(), workshopA.getId())).isPresent();
	}

	@Test
	@DisplayName("a carga detalhada da OS tambem respeita a fronteira da oficina")
	void detailedWorkOrderLoadIsScopedToWorkshop() {
		// A carga com itens e um caminho separado no repositorio; se so o metodo
		// simples fosse protegido, a tela de detalhe seria a porta aberta.
		assertThat(workOrderRepository.findDetailedByIdAndWorkshopId(orderA.getId(), workshopB.getId())).isEmpty();
		assertThat(workOrderRepository.findDetailedByIdAndWorkshopId(orderA.getId(), workshopA.getId())).isPresent();
	}

	@Test
	@DisplayName("a numeracao de OS reinicia em cada oficina")
	void workOrderNumberingIsPerWorkshop() {
		// Numero sequencial por oficina: a oficina B nao pode herdar a contagem da
		// A, nem deduzir por ela quantas ordens a concorrente ja abriu.
		assertThat(workOrderRepository.nextNumber(workshopA.getId())).isEqualTo(2);
		assertThat(workOrderRepository.nextNumber(workshopB.getId())).isEqualTo(1);
	}

	// ------------------------------------------------------------- fixtures
	private Workshop createWorkshop(String name, String cnpj, String ownerEmail) {
		User owner = new User();
		owner.setFirstName("Dono");
		owner.setLastName(name);
		owner.setEmail(ownerEmail);
		owner.setPassword("hash-irrelevante-para-este-teste");
		owner.setActive(true);
		userRepository.save(owner);

		Workshop workshop = new Workshop();
		workshop.setTradeName(name);
		workshop.setCnpj(cnpj);
		workshop.setActive(true);
		workshop.setOwner(owner);
		workshopRepository.save(workshop);

		return workshop;
	}

	private Customer createCustomer(Workshop workshop, String name, String document) {
		Customer customer = new Customer();
		customer.setWorkshop(workshop);
		customer.setType(CustomerType.INDIVIDUAL);
		customer.setName(name);
		customer.setDocument(document);
		customer.setActive(true);
		return customerRepository.save(customer);
	}

	private Vehicle createVehicle(Workshop workshop, Customer customer, String plate) {
		Vehicle vehicle = new Vehicle();
		vehicle.setWorkshop(workshop);
		vehicle.setCustomer(customer);
		vehicle.setPlate(plate);
		vehicle.setBrand("Marca");
		vehicle.setModel("Modelo");
		vehicle.setActive(true);
		return vehicleRepository.save(vehicle);
	}

	private WorkOrder createWorkOrder(Workshop workshop, Customer customer, Vehicle vehicle) {
		WorkOrder order = new WorkOrder();
		order.setWorkshop(workshop);
		order.setNumber(1);
		order.setCustomer(customer);
		order.setVehicle(vehicle);
		order.setStatus(WorkOrderStatus.OPEN);
		order.setQuoteStatus(QuoteStatus.NOT_REQUIRED);
		order.setOpenedAt(LocalDateTime.now());
		order.setDiscount(BigDecimal.ZERO);
		return workOrderRepository.save(order);
	}
}
