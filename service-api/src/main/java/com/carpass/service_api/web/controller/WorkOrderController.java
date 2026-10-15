package com.carpass.service_api.web.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.WorkOrderService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.workorder.NoteRequest;
import com.carpass.service_api.web.dto.workorder.QuoteDeliveryRequest;
import com.carpass.service_api.web.dto.workorder.StatusChangeRequest;
import com.carpass.service_api.web.dto.workorder.WorkOrderCreateRequest;
import com.carpass.service_api.web.dto.workorder.WorkOrderItemRequest;
import com.carpass.service_api.web.dto.workorder.WorkOrderResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderStatusHistoryResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderSummaryResponse;
import com.carpass.service_api.web.dto.workorder.WorkOrderUpdateRequest;

import jakarta.validation.Valid;

/**
 * Ordens de servico.
 *
 * <p>
 * Mecanicos consultam e movimentam o status do que executam; aprovar, recusar e
 * reabrir orcamento ficam com quem responde pelo dinheiro.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/work-orders")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class WorkOrderController {

	private final WorkOrderService workOrderService;

	public WorkOrderController(WorkOrderService workOrderService) {
		this.workOrderService = workOrderService;
	}

	/**
	 * Lista paginada com busca por numero da OS, cliente, placa ou modelo.
	 *
	 * @param from data inicial de abertura, no formato aaaa-MM-dd
	 * @param to   data final de abertura, no formato aaaa-MM-dd
	 */
	@GetMapping
	public ResponseEntity<PageResponse<WorkOrderSummaryResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) WorkOrderStatus status,
			@RequestParam(required = false) UUID mechanicId,
			@RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) UUID vehicleId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {

		return ResponseEntity.ok(workOrderService.search(q, status, mechanicId, customerId, vehicleId, from, to,
				pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<WorkOrderResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(workOrderService.findById(id));
	}

	/** Linha do tempo de mudancas de status, com autor e horario. */
	@GetMapping("/{id}/history")
	public ResponseEntity<List<WorkOrderStatusHistoryResponse>> history(@PathVariable UUID id) {
		return ResponseEntity.ok(workOrderService.findStatusHistory(id));
	}

	@PostMapping
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<WorkOrderResponse> create(@RequestBody @Valid WorkOrderCreateRequest request) {
		WorkOrderResponse created = workOrderService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/work-orders/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<WorkOrderResponse> update(@PathVariable UUID id,
			@RequestBody @Valid WorkOrderUpdateRequest request) {
		return ResponseEntity.ok(workOrderService.update(id, request));
	}

	// -------------------------------------------------------------- itens
	/** Adiciona um servico ou peca. Peca sai do estoque neste momento. */
	@PostMapping("/{id}/items")
	public ResponseEntity<WorkOrderResponse> addItem(@PathVariable UUID id,
			@RequestBody @Valid WorkOrderItemRequest request) {
		return ResponseEntity.ok(workOrderService.addItem(id, request));
	}

	@PutMapping("/{id}/items/{itemId}")
	public ResponseEntity<WorkOrderResponse> updateItem(@PathVariable UUID id, @PathVariable UUID itemId,
			@RequestBody @Valid WorkOrderItemRequest request) {
		return ResponseEntity.ok(workOrderService.updateItem(id, itemId, request));
	}

	/** Remove o item e devolve a peca ao estoque. */
	@DeleteMapping("/{id}/items/{itemId}")
	public ResponseEntity<WorkOrderResponse> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
		return ResponseEntity.ok(workOrderService.removeItem(id, itemId));
	}

	// ------------------------------------------------------------- status
	@PatchMapping("/{id}/status")
	public ResponseEntity<WorkOrderResponse> changeStatus(@PathVariable UUID id,
			@RequestBody @Valid StatusChangeRequest request) {
		return ResponseEntity.ok(workOrderService.changeStatus(id, request));
	}

	/**
	 * Marca como entregue e atualiza a quilometragem do veiculo.
	 *
	 * <p>
	 * O corpo e opcional, mas quando vem precisa ser validado: sem {@code @Valid}
	 * o {@code @Size} do {@link NoteRequest} nao e aplicado, e uma observacao de
	 * tamanho arbitrario chega ate o banco. Corpo ausente continua valendo --
	 * {@code @Valid} sobre nulo simplesmente nao roda.
	 * </p>
	 */
	@PostMapping("/{id}/deliver")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<WorkOrderResponse> deliver(@PathVariable UUID id, @RequestBody(required = false) @Valid NoteRequest request) {
		return ResponseEntity.ok(workOrderService.deliver(id, request));
	}

	/** Cancela a ordem e devolve ao estoque as pecas reservadas. */
	@PostMapping("/{id}/cancel")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<WorkOrderResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) @Valid NoteRequest request) {
		return ResponseEntity.ok(workOrderService.cancel(id, request));
	}

	// ---------------------------------------------------------- orcamento
	/**
	 * Fecha o orcamento, coloca a ordem em aprovacao e envia a proposta ao cliente.
	 *
	 * <p>
	 * O corpo e opcional e serve para informar um e-mail so daquele envio, sem
	 * alterar o cadastro do cliente. Sem e-mail nenhum a ordem entra em aprovacao
	 * assim mesmo, e a decisao fica a cargo da gerencia.
	 * </p>
	 */
	@PostMapping("/{id}/quote/submit")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<WorkOrderResponse> submitQuote(@PathVariable UUID id,
			@RequestBody(required = false) @Valid QuoteDeliveryRequest request) {
		return ResponseEntity.ok(workOrderService.submitQuote(id, request == null ? null : request.email()));
	}

	/**
	 * Reenvia a proposta ao cliente.
	 *
	 * <p>
	 * Reenviar nao e decidir, entao continua sendo operacao de balcao. O link
	 * anterior deixa de valer, para nao existirem dois caminhos validos para
	 * responder a mesma proposta.
	 * </p>
	 */
	@PostMapping("/{id}/quote/resend")
	@PreAuthorize(RoleExpressions.FRONT_DESK)
	public ResponseEntity<WorkOrderResponse> resendQuote(@PathVariable UUID id,
			@RequestBody(required = false) @Valid QuoteDeliveryRequest request) {
		return ResponseEntity.ok(workOrderService.resendQuote(id, request == null ? null : request.email()));
	}

	/**
	 * Registra a aprovacao do cliente.
	 *
	 * <p>
	 * Restrito a gerencia de proposito. Quem aprova de verdade e o cliente, pelo
	 * link que recebe por e-mail; esta rota existe para o caso em que ele nao
	 * consegue usar o link e pede a um superior que registre a decisao por ele.
	 * O atendente nao entra nesse caminho -- aprovar valor e decisao de quem
	 * responde pelo dinheiro.
	 * </p>
	 */
	@PostMapping("/{id}/quote/approve")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<WorkOrderResponse> approveQuote(@PathVariable UUID id) {
		return ResponseEntity.ok(workOrderService.approveQuote(id));
	}

	/** Registra a recusa do cliente. Mesma regra da aprovacao. */
	@PostMapping("/{id}/quote/reject")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<WorkOrderResponse> rejectQuote(@PathVariable UUID id,
			@RequestBody(required = false) @Valid NoteRequest request) {
		return ResponseEntity.ok(workOrderService.rejectQuote(id, request));
	}

	/** Reabre um orcamento aprovado para permitir ajuste de itens. */
	@PostMapping("/{id}/quote/reopen")
	@PreAuthorize(RoleExpressions.MANAGEMENT)
	public ResponseEntity<WorkOrderResponse> reopenQuote(@PathVariable UUID id,
			@RequestBody(required = false) @Valid NoteRequest request) {
		return ResponseEntity.ok(workOrderService.reopenQuote(id, request));
	}
}
