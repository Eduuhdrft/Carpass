package com.carpass.service_api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.workorder.NoteRequest;
import com.carpass.service_api.web.dto.workorder.StatusChangeRequest;
import com.carpass.service_api.web.dto.workorder.WorkOrderCreateRequest;
import com.carpass.service_api.web.dto.workorder.WorkOrderItemRequest;
import com.carpass.service_api.web.dto.workorder.PublicQuoteResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderStatusHistoryResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderSummaryResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderUpdateRequest;

/**
 * Ordens de servico.
 */
public interface WorkOrderService {

	PageResponse<WorkOrderSummaryResponse> search(String term, WorkOrderStatus status, UUID mechanicId, UUID customerId,
			UUID vehicleId, LocalDate from, LocalDate to, Pageable pageable);

	WorkOrderResponse findById(UUID id);

	List<WorkOrderStatusHistoryResponse> findStatusHistory(UUID id);

	WorkOrderResponse create(WorkOrderCreateRequest request);

	WorkOrderResponse update(UUID id, WorkOrderUpdateRequest request);

	// ------------------------------------------------------------- itens
	WorkOrderResponse addItem(UUID id, WorkOrderItemRequest request);

	WorkOrderResponse updateItem(UUID id, UUID itemId, WorkOrderItemRequest request);

	WorkOrderResponse removeItem(UUID id, UUID itemId);

	// ------------------------------------------------------------ status
	WorkOrderResponse changeStatus(UUID id, StatusChangeRequest request);

	/** Marca como entregue, registra a data e atualiza a quilometragem do veiculo. */
	WorkOrderResponse deliver(UUID id, NoteRequest request);

	/** Cancela a ordem e devolve ao estoque todas as pecas reservadas. */
	WorkOrderResponse cancel(UUID id, NoteRequest request);

	/**
	 * Encerra as ordens abertas paradas ha mais tempo que o limite da oficina.
	 *
	 * <p>
	 * Chamado pela rotina agendada, nunca por um controller: nao existe endpoint
	 * para isso. E uma operacao que percorre todas as oficinas, e expo-la na API
	 * seria dar a uma pessoa a chave de encerrar ordens de outra.
	 * </p>
	 *
	 * @return quantas ordens foram encerradas
	 */
	int closeIdleOrders();

	// --------------------------------------------------------- orcamento
	/** Envia o orcamento para aprovacao do cliente. */
	/**
	 * Fecha o orcamento e envia a proposta ao cliente.
	 *
	 * @param email destino do envio; quando nulo usa o e-mail do cadastro do
	 *              cliente. Sem nenhum dos dois, a ordem entra em aprovacao mas
	 *              nada e enviado, e so a gerencia registra a decisao.
	 */
	WorkOrderResponse submitQuote(UUID id, String email);

	/** Emite um novo link e reenvia a proposta, derrubando o link anterior. */
	WorkOrderResponse resendQuote(UUID id, String email);

	/** Orcamento como o cliente ve, a partir do token do link. */
	PublicQuoteResponse findQuoteByToken(String token);

	/** Aprovacao feita pelo proprio cliente, pelo link do e-mail. */
	PublicQuoteResponse approveQuoteByToken(String token);

	/** Recusa feita pelo proprio cliente, pelo link do e-mail. */
	PublicQuoteResponse rejectQuoteByToken(String token, String note);

	WorkOrderResponse approveQuote(UUID id);

	WorkOrderResponse rejectQuote(UUID id, NoteRequest request);

	/** Reabre o orcamento aprovado para permitir ajuste de itens. */
	WorkOrderResponse reopenQuote(UUID id, NoteRequest request);
}
