package com.carpass.service_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.repository.CustomerRepository;
import com.carpass.service_api.repository.EmployeeRepository;
import com.carpass.service_api.repository.PartRepository;
import com.carpass.service_api.repository.VehicleRepository;
import com.carpass.service_api.repository.WorkOrderRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.service.DashboardService;
import com.carpass.service_api.web.dto.dashboard.DashboardResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderSummaryResponse;

/**
 * Monta a visao geral da oficina.
 *
 * <p>
 * Tudo aqui e agregacao feita no banco. Nenhuma contagem carrega a lista inteira
 * para contar em memoria, o que manteria o painel rapido mesmo com a base
 * grande.
 * </p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

	/** Quantos itens mostrar nas listas curtas do painel. */
	private static final int SHORT_LIST_SIZE = 5;

	/** Janela considerada como "entrega proxima". */
	private static final int UPCOMING_DELIVERY_DAYS = 7;

	private final WorkOrderRepository workOrderRepository;
	private final CustomerRepository customerRepository;
	private final VehicleRepository vehicleRepository;
	private final EmployeeRepository employeeRepository;
	private final PartRepository partRepository;
	private final FinanceAccess financeAccess;
	private final CurrentUser currentUser;

	public DashboardServiceImpl(WorkOrderRepository workOrderRepository, CustomerRepository customerRepository,
			VehicleRepository vehicleRepository, EmployeeRepository employeeRepository, PartRepository partRepository,
			FinanceAccess financeAccess, CurrentUser currentUser) {
		this.workOrderRepository = workOrderRepository;
		this.customerRepository = customerRepository;
		this.vehicleRepository = vehicleRepository;
		this.employeeRepository = employeeRepository;
		this.partRepository = partRepository;
		this.financeAccess = financeAccess;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardResponse load() {
		UUID workshopId = currentUser.requireWorkshopId();

		DashboardResponse.Totals totals = new DashboardResponse.Totals(
				customerRepository.countByWorkshopIdAndActiveTrue(workshopId),
				vehicleRepository.countByWorkshopIdAndActiveTrue(workshopId),
				employeeRepository.countByWorkshopIdAndActiveTrue(workshopId),
				workOrderRepository.countOpen(workshopId),
				partRepository.countLowStock(workshopId));

		// Faturamento e informacao de gestao. Atendente e mecanico recebem null, e
		// a tela nem desenha o cartao. Esconder so no frontend nao serviria: o
		// numero continuaria viajando na resposta, visivel para quem abrisse as
		// ferramentas do navegador.
		BigDecimal revenue = canSeeRevenue() ? monthRevenue(workshopId) : null;

		return new DashboardResponse(totals, ordersByStatus(workshopId), revenue,
				topServices(workshopId), recentOrders(workshopId), upcomingDeliveries(workshopId));
	}

	/**
	 * Quem responde pelo dinheiro da oficina.
	 *
	 * <p>
	 * A regra mora no {@code FinanceAccess}, junto com a da tela financeira: o
	 * dono sempre ve, e o gerente ve se o dono deixar. Duas copias da mesma regra
	 * divergiriam no dia em que uma delas mudasse -- e o sintoma seria o gerente
	 * perdendo o financeiro mas continuando a ver o faturamento aqui.
	 * </p>
	 */
	private boolean canSeeRevenue() {
		return financeAccess.canView();
	}

	/** Contagem por status, incluindo os status com zero para o grafico nao "pular" faixas. */
	private List<DashboardResponse.StatusCount> ordersByStatus(UUID workshopId) {
		Map<WorkOrderStatus, Long> counts = new EnumMap<>(WorkOrderStatus.class);
		for (Object[] row : workOrderRepository.countGroupedByStatus(workshopId)) {
			counts.put((WorkOrderStatus) row[0], ((Number) row[1]).longValue());
		}

		List<DashboardResponse.StatusCount> result = new ArrayList<>();
		for (WorkOrderStatus status : WorkOrderStatus.values()) {
			result.add(new DashboardResponse.StatusCount(status, status.getLabel(), counts.getOrDefault(status, 0L)));
		}
		return result;
	}

	/** Faturamento das ordens entregues do primeiro dia do mes ate agora. */
	private BigDecimal monthRevenue(UUID workshopId) {
		LocalDate today = LocalDate.now();
		LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime to = today.atTime(LocalTime.MAX);

		BigDecimal revenue = workOrderRepository.sumRevenueBetween(workshopId, from, to);
		return revenue == null ? BigDecimal.ZERO : revenue;
	}

	private List<DashboardResponse.TopService> topServices(UUID workshopId) {
		List<DashboardResponse.TopService> result = new ArrayList<>();
		for (Object[] row : workOrderRepository.findTopServices(workshopId, PageRequest.of(0, SHORT_LIST_SIZE))) {
			result.add(new DashboardResponse.TopService((String) row[0], ((Number) row[1]).longValue()));
		}
		return result;
	}

	private List<WorkOrderSummaryResponse> recentOrders(UUID workshopId) {
		return workOrderRepository.findRecent(workshopId, PageRequest.of(0, SHORT_LIST_SIZE)).stream()
				.map(WorkOrderSummaryResponse::from).toList();
	}

	private List<WorkOrderSummaryResponse> upcomingDeliveries(UUID workshopId) {
		LocalDate until = LocalDate.now().plusDays(UPCOMING_DELIVERY_DAYS);
		return workOrderRepository.findUpcomingDeliveries(workshopId, until, PageRequest.of(0, SHORT_LIST_SIZE))
				.stream().map(WorkOrderSummaryResponse::from).toList();
	}
}
