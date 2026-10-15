package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.enums.CustomerType;
import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.domain.model.Employee;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.repository.spec.WorkOrderSpecifications;

/**
 * Busca de ordens de servico.
 *
 * <p>
 * O caso que motivou esta classe: a listagem sem filtro nenhum caia com 500 e a
 * OS recem-aberta nao aparecia. A consulta usava o idioma
 * <code>(:param is null or coluna = :param)</code>, que o PostgreSQL nao
 * consegue analisar -- e, de quebra, criava um <em>inner join</em> implicito ao
 * dereferenciar o mecanico, relacao que e opcional.
 * </p>
 *
 * <p>
 * Hoje o filtro e montado por {@code WorkOrderSpecifications}: filtro ausente
 * nao vira clausula. Os testes abaixo travam a semantica -- sem filtro de
 * mecanico a listagem devolve tudo, inclusive a OS que ainda nao tem mecanico;
 * com filtro, devolve so a daquele mecanico.
 * </p>
 *
 * <p>
 * <strong>Limite honesto deste teste:</strong> ele roda em H2, que aceitava o
 * SQL que o PostgreSQL recusava. Ou seja, ele nao teria pego o 500. O que
 * garante que aquele erro nao volta nao e este teste, e sim o fato de nenhum
 * parametro nulo chegar mais ao banco.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkOrderSearchTest {

	@Autowired
	private WorkshopRepository workshopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private WorkOrderRepository workOrderRepository;

	private Workshop workshop;
	private Employee mechanic;

	@BeforeEach
	void setUp() {
		workshop = createWorkshop();
		Customer customer = createCustomer(workshop);
		Vehicle vehicle = createVehicle(workshop, customer, "ABC1D23");
		mechanic = createMechanic(workshop);

		// Os numeros nao podem conter digito que apareca na placa: a busca por
		// texto casa numero da OS *e* placa, e as duas ordens dividem o mesmo
		// carro. Com numeros 1 e 2, buscar "1" casaria a placa ABC1D23 e traria
		// as duas -- foi assim que este teste nasceu quebrado.
		createWorkOrder(workshop, customer, vehicle, 1040, null);
		createWorkOrder(workshop, customer, vehicle, 1041, mechanic);
	}

	@Test
	@DisplayName("sem filtro nenhum, a listagem inclui a OS que ainda nao tem mecanico")
	void listsOrdersWithoutAssignedMechanic() {
		var page = workOrderRepository.findAll(noFilters(), PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).extracting(WorkOrder::getNumber).containsExactlyInAnyOrder(1040, 1041);
	}

	@Test
	@DisplayName("a contagem da pagina bate com o conteudo devolvido")
	void countMatchesContent() {
		var page = workOrderRepository.findAll(noFilters(), PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(page.getContent().size());
	}

	@Test
	@DisplayName("com filtro de mecanico, devolve so as ordens daquele mecanico")
	void filtersByMechanicWhenAsked() {
		var filter = WorkOrderSpecifications.filter(workshop.getId(), null, null, mechanic.getId(), null, null, null,
				null);

		assertThat(workOrderRepository.findAll(filter, PageRequest.of(0, 20)).getContent()).singleElement()
				.satisfies(order -> assertThat(order.getNumber()).isEqualTo(1041));
	}

	@Test
	@DisplayName("buscar pelo numero da OS encontra a ordem sem mecanico")
	void findsOrderWithoutMechanicByNumber() {
		var filter = WorkOrderSpecifications.filter(workshop.getId(), "1040", null, null, null, null, null, null);

		assertThat(workOrderRepository.findAll(filter, PageRequest.of(0, 20)).getContent()).singleElement()
				.satisfies(order -> assertThat(order.getNumber()).isEqualTo(1040));
	}

	@Test
	@DisplayName("buscar pela placa traz todas as ordens daquele carro")
	void findsEveryOrderOfTheSameVehicleByPlate() {
		var filter = WorkOrderSpecifications.filter(workshop.getId(), "abc1d23", null, null, null, null, null, null);

		// Buscar placa devolver o historico inteiro do carro e o comportamento
		// desejado, e a busca ignora maiusculas.
		assertThat(workOrderRepository.findAll(filter, PageRequest.of(0, 20)).getContent())
				.extracting(WorkOrder::getNumber).containsExactlyInAnyOrder(1040, 1041);
	}

	@Test
	@DisplayName("a listagem nao atravessa a fronteira da oficina")
	void doesNotLeakAcrossWorkshops() {
		var filter = WorkOrderSpecifications.filter(UUID.randomUUID(), null, null, null, null, null, null, null);

		assertThat(workOrderRepository.findAll(filter, PageRequest.of(0, 20)).getTotalElements()).isZero();
	}

	private Specification<WorkOrder> noFilters() {
		return WorkOrderSpecifications.filter(workshop.getId(), null, null, null, null, null, null, null);
	}

	// ------------------------------------------------------------- fixtures
	private Workshop createWorkshop() {
		User owner = new User();
		owner.setFirstName("Dono");
		owner.setLastName("Da Oficina");
		owner.setEmail("dono@teste.com");
		owner.setPassword("hash-irrelevante-para-este-teste");
		owner.setActive(true);
		userRepository.save(owner);

		Workshop created = new Workshop();
		created.setTradeName("Oficina de Teste");
		created.setCnpj("11111111000191");
		created.setActive(true);
		created.setOwner(owner);
		workshopRepository.save(created);

		return created;
	}

	private Customer createCustomer(Workshop owner) {
		Customer customer = new Customer();
		customer.setWorkshop(owner);
		customer.setType(CustomerType.INDIVIDUAL);
		customer.setName("Joao da Silva");
		customer.setDocument("11122233344");
		customer.setActive(true);
		return customerRepository.save(customer);
	}

	private Vehicle createVehicle(Workshop owner, Customer customer, String plate) {
		Vehicle vehicle = new Vehicle();
		vehicle.setWorkshop(owner);
		vehicle.setCustomer(customer);
		vehicle.setPlate(plate);
		vehicle.setBrand("Fiat");
		vehicle.setModel("Argo");
		vehicle.setActive(true);
		return vehicleRepository.save(vehicle);
	}

	private Employee createMechanic(Workshop owner) {
		Employee employee = new Employee();
		employee.setWorkshop(owner);
		employee.setName("Carlos Souza");
		employee.setRole(Role.MECHANIC);
		employee.setActive(true);
		return employeeRepository.save(employee);
	}

	private WorkOrder createWorkOrder(Workshop owner, Customer customer, Vehicle vehicle, int number,
			Employee assignedMechanic) {
		WorkOrder order = new WorkOrder();
		order.setWorkshop(owner);
		order.setNumber(number);
		order.setCustomer(customer);
		order.setVehicle(vehicle);
		order.setAssignedMechanic(assignedMechanic);
		order.setStatus(WorkOrderStatus.OPEN);
		order.setQuoteStatus(QuoteStatus.NOT_REQUIRED);
		order.setOpenedAt(LocalDateTime.now());
		order.setDiscount(BigDecimal.ZERO);
		return workOrderRepository.save(order);
	}
}
