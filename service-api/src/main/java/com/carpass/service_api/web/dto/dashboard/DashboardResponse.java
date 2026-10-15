package com.carpass.service_api.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.web.dto.workorder.WorkOrderSummaryResponse;

/**
 * Visao geral da operacao da oficina.
 *
 * <p>
 * Reune em uma unica chamada os numeros que a tela inicial mostra, para o painel
 * nao precisar disparar oito requisicoes ao abrir.
 * </p>
 */
public record DashboardResponse(Totals totals, List<StatusCount> ordersByStatus, BigDecimal monthRevenue,
		List<TopService> topServices, List<WorkOrderSummaryResponse> recentOrders,
		List<WorkOrderSummaryResponse> upcomingDeliveries) {

	/**
	 * Contagens gerais.
	 *
	 * @param openWorkOrders ordens que ainda ocupam a oficina
	 * @param lowStockParts  pecas no estoque minimo ou abaixo dele
	 */
	public record Totals(long customers, long vehicles, long employees, long openWorkOrders, long lowStockParts) {
	}

	/** Quantidade de ordens em cada status. */
	public record StatusCount(WorkOrderStatus status, String label, long count) {
	}

	/** Servico mais executado e quantas vezes apareceu. */
	public record TopService(String description, long count) {
	}
}
