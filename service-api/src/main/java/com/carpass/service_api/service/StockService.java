package com.carpass.service_api.service;

import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartStockMovement;
import com.carpass.service_api.domain.model.WorkOrder;

/**
 * Ponto unico de alteracao de saldo de estoque.
 *
 * <p>
 * Nenhum outro servico deve escrever {@code stockQuantity} diretamente. Passar
 * tudo por aqui garante que todo saldo tenha uma movimentacao correspondente e
 * que a regra de estoque negativo seja aplicada em qualquer caminho, seja um
 * ajuste manual ou o consumo de uma peca em uma ordem de servico.
 * </p>
 */
public interface StockService {

	/**
	 * Aplica uma movimentacao e atualiza o saldo da peca.
	 *
	 * @param part      peca afetada, ja carregada e pertencente a oficina atual
	 * @param type      entrada, saida ou ajuste
	 * @param quantity  quantidade movimentada; no ajuste, e o novo saldo absoluto
	 * @param reason    motivo livre, exibido no historico
	 * @param workOrder ordem de servico que originou a movimentacao, se houver
	 * @return a movimentacao registrada
	 */
	PartStockMovement register(Part part, StockMovementType type, int quantity, String reason, WorkOrder workOrder);
}
