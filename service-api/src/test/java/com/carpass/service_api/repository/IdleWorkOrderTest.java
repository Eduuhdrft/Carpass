package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.enums.CustomerType;
import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.domain.model.Workshop;

import jakarta.persistence.EntityManager;

/**
 * A consulta que sustenta o encerramento automatico.
 *
 * <p>
 * E uma rotina que <strong>cancela documentos sozinha</strong>. Um erro aqui nao
 * aparece como erro: aparece como ordem encerrada que ainda estava viva, e a
 * oficina descobre pelo cliente. Por isso os testes cobrem tanto o que deve ser
 * alcancado quanto o que nao pode ser tocado.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class IdleWorkOrderTest {

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

	@Autowired
	private EntityManager entityManager;

	private Workshop oficina;
	private Workshop vizinha;
	private Customer cliente;
	private Vehicle carro;

	@BeforeEach
	void setUp() {
		oficina = criarOficina("Oficina de Teste", "11111111000191", "dono@teste.com");
		vizinha = criarOficina("Oficina Vizinha", "22222222000192", "vizinho@teste.com");
		cliente = criarCliente(oficina);
		carro = criarVeiculo(oficina, cliente, "ABC1D23");
	}

	@Test
	@DisplayName("ordem aberta e parada ha mais tempo que o limite e alcancada")
	void findsIdleOpenOrder() {
		criarOrdem(1001, WorkOrderStatus.OPEN, horasAtras(72));

		var paradas = workOrderRepository.findIdleOpenOrders(oficina.getId(), LocalDateTime.now().minusHours(48));

		assertThat(paradas).extracting(WorkOrder::getNumber).containsExactly(1001);
	}

	@Test
	@DisplayName("ordem mexida recentemente nao e alcancada, por mais antiga que seja")
	void ignoresRecentlyTouchedOrder() {
		// Aberta ha uma semana, mexida ha duas horas: nao esta parada. O relogio
		// conta a partir da ultima movimentacao, e nao da abertura -- confundir os
		// dois encerraria uma OS em pleno andamento.
		criarOrdem(1002, WorkOrderStatus.OPEN, horasAtras(2));

		var paradas = workOrderRepository.findIdleOpenOrders(oficina.getId(), LocalDateTime.now().minusHours(48));

		assertThat(paradas).isEmpty();
	}

	@Test
	@DisplayName("so ordens ABERTAS entram: em execucao e aguardando peca ficam de fora")
	void onlyOpenOrdersAreEligible() {
		criarOrdem(1003, WorkOrderStatus.IN_PROGRESS, horasAtras(200));
		criarOrdem(1004, WorkOrderStatus.WAITING_PARTS, horasAtras(200));
		criarOrdem(1005, WorkOrderStatus.WAITING_APPROVAL, horasAtras(200));

		// Uma OS aguardando peca fica parada por semanas de forma legitima, e
		// "em execucao" tem carro no elevador. Encerrar qualquer uma das duas
		// seria o pior erro possivel desta rotina.
		var paradas = workOrderRepository.findIdleOpenOrders(oficina.getId(), LocalDateTime.now().minusHours(48));

		assertThat(paradas).isEmpty();
	}

	@Test
	@DisplayName("ordem ja encerrada nao volta a ser alcancada")
	void ignoresFinishedOrders() {
		criarOrdem(1006, WorkOrderStatus.DELIVERED, horasAtras(500));
		criarOrdem(1007, WorkOrderStatus.CANCELED, horasAtras(500));

		assertThat(workOrderRepository.findIdleOpenOrders(oficina.getId(), LocalDateTime.now().minusHours(48)))
				.isEmpty();
	}

	@Test
	@DisplayName("a varredura nao atravessa a fronteira da oficina")
	void doesNotLeakAcrossWorkshops() {
		Customer daVizinha = criarCliente(vizinha);
		Vehicle carroDaVizinha = criarVeiculo(vizinha, daVizinha, "QRS4B11");

		WorkOrder ordemDaVizinha = new WorkOrder();
		ordemDaVizinha.setWorkshop(vizinha);
		ordemDaVizinha.setNumber(9001);
		ordemDaVizinha.setCustomer(daVizinha);
		ordemDaVizinha.setVehicle(carroDaVizinha);
		ordemDaVizinha.setStatus(WorkOrderStatus.OPEN);
		ordemDaVizinha.setQuoteStatus(QuoteStatus.NOT_REQUIRED);
		ordemDaVizinha.setOpenedAt(horasAtras(200));
		ordemDaVizinha.setDiscount(BigDecimal.ZERO);
		workOrderRepository.save(ordemDaVizinha);
		envelhecer(ordemDaVizinha, horasAtras(200));

		// Uma rotina que roda para todas as oficinas e o lugar mais facil de
		// esquecer o escopo: ela ja esta iterando o sistema inteiro.
		assertThat(workOrderRepository.findIdleOpenOrders(oficina.getId(), LocalDateTime.now().minusHours(48)))
				.isEmpty();
		assertThat(workOrderRepository.findIdleOpenOrders(vizinha.getId(), LocalDateTime.now().minusHours(48)))
				.extracting(WorkOrder::getNumber).containsExactly(9001);
	}

	@Test
	@DisplayName("a busca de oficinas so traz quem ligou a rotina")
	void onlyWorkshopsWithTheSettingOn() {
		oficina.setAutoCloseOpenOrdersHours(48);
		workshopRepository.save(oficina);

		// A vizinha fica com null: quem nao ligou nao e nem visitado.
		assertThat(workshopRepository.findByActiveTrueAndAutoCloseOpenOrdersHoursNotNull())
				.extracting(Workshop::getTradeName)
				.containsExactly("Oficina de Teste");
	}

	@Test
	@DisplayName("oficina desativada fica de fora da varredura")
	void inactiveWorkshopIsSkipped() {
		oficina.setAutoCloseOpenOrdersHours(48);
		oficina.setActive(false);
		workshopRepository.save(oficina);

		assertThat(workshopRepository.findByActiveTrueAndAutoCloseOpenOrdersHoursNotNull()).isEmpty();
	}

	@Test
	@DisplayName("o padrao e desligado, inclusive para oficina recem-criada")
	void defaultIsDisabled() {
		// Encerrar documento sem a oficina ter pedido e o tipo de automacao que,
		// quando erra uma vez, faz a pessoa deixar de confiar no sistema.
		assertThat(oficina.getAutoCloseOpenOrdersHours()).isNull();
		assertThat(workshopRepository.findByActiveTrueAndAutoCloseOpenOrdersHoursNotNull()).isEmpty();
	}

	// ------------------------------------------------------------- fixtures
	private LocalDateTime horasAtras(int horas) {
		return LocalDateTime.now().minusHours(horas);
	}

	private WorkOrder criarOrdem(int numero, WorkOrderStatus status, LocalDateTime ultimaMovimentacao) {
		WorkOrder order = new WorkOrder();
		order.setWorkshop(oficina);
		order.setNumber(numero);
		order.setCustomer(cliente);
		order.setVehicle(carro);
		order.setStatus(status);
		order.setQuoteStatus(QuoteStatus.NOT_REQUIRED);
		order.setOpenedAt(ultimaMovimentacao);
		order.setDiscount(BigDecimal.ZERO);
		workOrderRepository.save(order);

		envelhecer(order, ultimaMovimentacao);
		return order;
	}

	/**
	 * Empurra o {@code updated_at} para tras.
	 *
	 * <p>
	 * O Hibernate preenche esse campo sozinho a cada gravacao, entao nao ha como
	 * criar uma linha "velha" pelo caminho normal. O UPDATE direto e a unica forma
	 * de montar o cenario que a rotina existe para tratar.
	 * </p>
	 */
	private void envelhecer(WorkOrder order, LocalDateTime quando) {
		workOrderRepository.flush();
		entityManager.createNativeQuery("update work_orders set updated_at = ?1 where id = ?2")
				.setParameter(1, quando)
				.setParameter(2, order.getId())
				.executeUpdate();
		entityManager.clear();
	}

	private Workshop criarOficina(String nome, String cnpj, String emailDoDono) {
		User dono = new User();
		dono.setFirstName("Dono");
		dono.setLastName(nome);
		dono.setEmail(emailDoDono);
		dono.setPassword("hash-irrelevante-para-este-teste");
		dono.setActive(true);
		userRepository.save(dono);

		Workshop criada = new Workshop();
		criada.setTradeName(nome);
		criada.setCnpj(cnpj);
		criada.setActive(true);
		criada.setOwner(dono);
		workshopRepository.save(criada);

		return criada;
	}

	private Customer criarCliente(Workshop dona) {
		Customer novo = new Customer();
		novo.setWorkshop(dona);
		novo.setType(CustomerType.INDIVIDUAL);
		novo.setName("Joao da Silva");
		novo.setDocument("11122233344");
		novo.setActive(true);
		return customerRepository.save(novo);
	}

	private Vehicle criarVeiculo(Workshop dona, Customer doCliente, String placa) {
		Vehicle veiculo = new Vehicle();
		veiculo.setWorkshop(dona);
		veiculo.setCustomer(doCliente);
		veiculo.setPlate(placa);
		veiculo.setBrand("Fiat");
		veiculo.setModel("Argo");
		veiculo.setActive(true);
		return vehicleRepository.save(veiculo);
	}
}
