package com.carpass.service_api.service;

import java.util.UUID;

/**
 * Avisa as telas abertas de que algo mudou na oficina.
 *
 * <p>
 * O aviso e deliberadamente magro: tipo do evento e o minimo para a tela decidir
 * se aquilo lhe interessa. Nenhum valor, nenhum dado de cliente. Quem recebe
 * busca pela API de sempre, que ja e escopada por oficina e respeita papel.
 * </p>
 *
 * <p>
 * Falha de entrega nunca derruba a operacao: se o aviso nao sair, a tela apenas
 * demora um pouco mais para saber -- o dado ja esta salvo no banco de qualquer
 * forma.
 * </p>
 */
public interface RealtimeNotifier {

	/** Tipos de aviso que as telas conhecem. */
	enum EventType {
		/** Ordem de servico mudou de etapa, de itens ou de valores. */
		WORK_ORDER_CHANGED,
		/** Cliente respondeu o orcamento pelo link do e-mail. */
		QUOTE_DECIDED,
		/** Peca chegou ao estoque minimo ou abaixo dele. */
		LOW_STOCK
	}

	/** Aviso sobre uma ordem de servico. */
	void workOrderChanged(UUID workshopId, UUID workOrderId, Integer number);

	/**
	 * Aviso de que o cliente respondeu o orcamento.
	 *
	 * @param approved true aprovou, false recusou
	 */
	void quoteDecided(UUID workshopId, UUID workOrderId, Integer number, boolean approved);

	/** Aviso de peca que precisa de reposicao. */
	void lowStock(UUID workshopId, UUID partId, String partName, int stockQuantity, int minimumStock);
}
