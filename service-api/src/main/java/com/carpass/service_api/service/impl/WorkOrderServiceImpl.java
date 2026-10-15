package com.carpass.service_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.ItemType;
import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.Employee;
import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.ServiceCatalog;
import com.carpass.service_api.domain.model.Vehicle;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.domain.model.WorkOrderItem;
import com.carpass.service_api.domain.model.QuoteApprovalToken;
import com.carpass.service_api.domain.model.WorkOrderStatusHistory;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.EmployeeRepository;
import com.carpass.service_api.repository.PartRepository;
import com.carpass.service_api.repository.ServiceCatalogRepository;
import com.carpass.service_api.repository.VehicleRepository;
import com.carpass.service_api.repository.WorkOrderRepository;
import com.carpass.service_api.repository.WorkOrderStatusHistoryRepository;
import com.carpass.service_api.repository.QuoteApprovalTokenRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.repository.spec.WorkOrderSpecifications;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.QuoteApprovalTokenIssuer;
import com.carpass.service_api.service.EmailService;
import com.carpass.service_api.service.RealtimeNotifier;
import com.carpass.service_api.service.StockService;
import com.carpass.service_api.service.WorkOrderService;
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
 * Regras da ordem de servico.
 *
 * <p>
 * Tres decisoes explicam quase todo o comportamento deste servico:
 * </p>
 *
 * <ul>
 * <li><b>A peca sai do estoque no momento em que entra na OS</b>, e nao no
 * fechamento. E assim que o atendente descobre na hora que a peca nao existe, em
 * vez de a oficina prometer um prazo e travar depois. Remover o item ou cancelar
 * a OS devolve tudo ao estoque.</li>
 * <li><b>Orcamento aprovado tranca os itens.</b> Para mexer, e preciso reabrir o
 * orcamento de forma explicita, o que devolve a OS para aprovacao do cliente e
 * fica registrado na linha do tempo. Ninguem altera em silencio um valor que o
 * cliente ja aceitou.</li>
 * <li><b>Toda mudanca de status passa pela maquina de estados</b> declarada em
 * {@link WorkOrderStatus}, e cada transicao vira uma linha de historico com
 * autor e horario.</li>
 * </ul>
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

	private static final Logger log = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

	private final WorkOrderRepository workOrderRepository;
	private final WorkOrderStatusHistoryRepository statusHistoryRepository;
	private final VehicleRepository vehicleRepository;
	private final EmployeeRepository employeeRepository;
	private final PartRepository partRepository;
	private final ServiceCatalogRepository serviceCatalogRepository;
	private final WorkshopRepository workshopRepository;
	private final StockService stockService;
	private final EmailService emailService;
	private final QuoteApprovalTokenIssuer quoteTokenIssuer;
	private final RealtimeNotifier realtimeNotifier;
	private final CurrentUser currentUser;

	public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository,
			WorkOrderStatusHistoryRepository statusHistoryRepository, VehicleRepository vehicleRepository,
			EmployeeRepository employeeRepository, PartRepository partRepository,
			ServiceCatalogRepository serviceCatalogRepository, WorkshopRepository workshopRepository,
			StockService stockService, EmailService emailService, QuoteApprovalTokenIssuer quoteTokenIssuer,
			RealtimeNotifier realtimeNotifier, CurrentUser currentUser) {
		this.workOrderRepository = workOrderRepository;
		this.statusHistoryRepository = statusHistoryRepository;
		this.vehicleRepository = vehicleRepository;
		this.employeeRepository = employeeRepository;
		this.partRepository = partRepository;
		this.serviceCatalogRepository = serviceCatalogRepository;
		this.workshopRepository = workshopRepository;
		this.stockService = stockService;
		this.emailService = emailService;
		this.quoteTokenIssuer = quoteTokenIssuer;
		this.realtimeNotifier = realtimeNotifier;
		this.currentUser = currentUser;
	}

	// ------------------------------------------------------------ leitura
	@Override
	@Transactional(readOnly = true)
	public PageResponse<WorkOrderSummaryResponse> search(String term, WorkOrderStatus status, UUID mechanicId,
			UUID customerId, UUID vehicleId, LocalDate from, LocalDate to, Pageable pageable) {

		Specification<WorkOrder> filter = WorkOrderSpecifications.filter(currentUser.requireWorkshopId(),
				normalize(term), status, mechanicId, customerId, vehicleId, startOfDay(from), endOfDay(to));

		return PageResponse.from(workOrderRepository.findAll(filter, pageable), WorkOrderSummaryResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public WorkOrderResponse findById(UUID id) {
		return respond(loadDetailed(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkOrderStatusHistoryResponse> findStatusHistory(UUID id) {
		load(id);
		return statusHistoryRepository.findByWorkOrderIdOrderByCreatedAtAsc(id).stream()
				.map(WorkOrderStatusHistoryResponse::from).toList();
	}

	// ------------------------------------------------------------ escrita
	@Override
	@Transactional
	public WorkOrderResponse create(WorkOrderCreateRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();

		Vehicle vehicle = vehicleRepository.findByIdAndWorkshopId(request.vehicleId(), workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Veiculo"));

		WorkOrder order = new WorkOrder();
		order.setWorkshop(workshopRepository.getReferenceById(workshopId));
		order.setNumber(workOrderRepository.nextNumber(workshopId));
		order.setVehicle(vehicle);
		// O cliente vem do veiculo: assim a OS nunca cobra de quem nao e o dono.
		order.setCustomer(vehicle.getCustomer());
		order.setStatus(WorkOrderStatus.OPEN);
		order.setQuoteStatus(QuoteStatus.NOT_REQUIRED);
		order.setOpenedAt(LocalDateTime.now());
		order.setReportedProblem(request.reportedProblem());
		order.setNotes(request.notes());
		order.setMileage(request.mileage() == null ? vehicle.getMileage() : request.mileage());
		order.setEstimatedDeliveryDate(request.estimatedDeliveryDate());
		order.setAssignedMechanic(resolveMechanic(request.assignedMechanicId(), workshopId));
		order.setDiscount(BigDecimal.ZERO);
		order.setCreatedBy(currentUser.requireUser());
		workOrderRepository.save(order);

		if (request.items() != null) {
			for (WorkOrderItemRequest item : request.items()) {
				order.addItem(buildItem(item, order, workshopId));
			}
		}

		recalculateTotals(order);
		workOrderRepository.save(order);
		recordHistory(order, null, WorkOrderStatus.OPEN, "Ordem de servico aberta");

		log.info("OS aberta: os={} numero={} veiculo={} oficina={}", order.getId(), order.getNumber(), vehicle.getId(),
				workshopId);
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse update(UUID id, WorkOrderUpdateRequest request) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		order.setAssignedMechanic(resolveMechanic(request.assignedMechanicId(), currentUser.requireWorkshopId()));
		order.setReportedProblem(request.reportedProblem());
		order.setDiagnosis(request.diagnosis());
		order.setNotes(request.notes());
		order.setEstimatedDeliveryDate(request.estimatedDeliveryDate());
		if (request.mileage() != null) {
			order.setMileage(request.mileage());
		}

		if (request.discount() != null && !request.discount().equals(order.getDiscount())) {
			// Desconto muda o total, entao vale a mesma trava do orcamento aprovado.
			requireItemsUnlocked(order);
			order.setDiscount(request.discount());
		}

		recalculateTotals(order);
		workOrderRepository.save(order);

		log.info("OS atualizada: os={}", id);
		return respond(order);
	}

	// -------------------------------------------------------------- itens
	@Override
	@Transactional
	public WorkOrderResponse addItem(UUID id, WorkOrderItemRequest request) {
		WorkOrder order = loadDetailed(id);
		requireItemsUnlocked(order);

		order.addItem(buildItem(request, order, currentUser.requireWorkshopId()));
		recalculateTotals(order);
		workOrderRepository.save(order);

		log.info("Item adicionado a OS: os={} tipo={}", id, request.type());
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse updateItem(UUID id, UUID itemId, WorkOrderItemRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		WorkOrder order = loadDetailed(id);
		requireItemsUnlocked(order);

		WorkOrderItem existing = findItem(order, itemId);
		// Devolve o que estava reservado e reserva de novo com os dados novos.
		releaseStock(existing, order);
		order.removeItem(existing);

		order.addItem(buildItem(request, order, workshopId));
		recalculateTotals(order);
		workOrderRepository.save(order);

		log.info("Item alterado na OS: os={} item={}", id, itemId);
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse removeItem(UUID id, UUID itemId) {
		WorkOrder order = loadDetailed(id);
		requireItemsUnlocked(order);

		WorkOrderItem item = findItem(order, itemId);
		releaseStock(item, order);
		order.removeItem(item);

		recalculateTotals(order);
		workOrderRepository.save(order);

		log.info("Item removido da OS: os={} item={}", id, itemId);
		return respond(order);
	}

	// ------------------------------------------------------------- status
	@Override
	@Transactional
	public WorkOrderResponse changeStatus(UUID id, StatusChangeRequest request) {
		WorkOrder order = loadDetailed(id);
		WorkOrderStatus target = request.status();

		if (target == WorkOrderStatus.DELIVERED) {
			return deliver(id, new NoteRequest(request.note()));
		}
		if (target == WorkOrderStatus.CANCELED) {
			return cancel(id, new NoteRequest(request.note()));
		}
		if (target == WorkOrderStatus.APPROVED) {
			throw new BusinessException(
					"Para aprovar a ordem, use a aprovacao do orcamento. Isso mantem o registro de quem aprovou.");
		}

		applyTransition(order, target, request.note());
		if (target == WorkOrderStatus.FINISHED) {
			order.setFinishedAt(LocalDateTime.now());
		}
		workOrderRepository.save(order);

		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse deliver(UUID id, NoteRequest request) {
		WorkOrder order = loadDetailed(id);

		applyTransition(order, WorkOrderStatus.DELIVERED, noteOr(request, "Veiculo entregue ao cliente"));
		order.setDeliveredAt(LocalDateTime.now());
		if (order.getFinishedAt() == null) {
			order.setFinishedAt(LocalDateTime.now());
		}

		// A quilometragem da OS e a leitura mais recente do veiculo.
		Vehicle vehicle = order.getVehicle();
		if (order.getMileage() != null
				&& (vehicle.getMileage() == null || order.getMileage() > vehicle.getMileage())) {
			vehicle.setMileage(order.getMileage());
			vehicleRepository.save(vehicle);
		}

		workOrderRepository.save(order);

		log.info("OS entregue: os={} numero={}", id, order.getNumber());
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse cancel(UUID id, NoteRequest request) {
		WorkOrder order = loadDetailed(id);
		applyCancellation(order, noteOr(request, "Ordem de servico cancelada"));
		return respond(order);
	}

	/**
	 * Encerra as ordens abertas que ninguem toca ha mais tempo que o limite da
	 * oficina.
	 *
	 * <h2>Por que passa pelo mesmo cancelamento do balcao</h2>
	 *
	 * <p>
	 * Uma rotina que escreve {@code status = CANCELED} por conta propria estaria
	 * pulando a devolucao das pecas ao estoque e o registro no historico -- e a
	 * divergencia so apareceria semanas depois, na contagem da prateleira. Aqui
	 * ela chama exatamente o que o atendente chamaria.
	 * </p>
	 *
	 * <h2>Sem usuario autenticado</h2>
	 *
	 * <p>
	 * A rotina roda fora de qualquer requisicao, entao nao ha usuario no
	 * contexto. O historico grava {@code changedBy} nulo e a nota diz que foi o
	 * sistema: a linha do tempo continua explicando o que aconteceu, sem atribuir
	 * a acao a uma pessoa que nao a tomou.
	 * </p>
	 *
	 * @return quantas ordens foram encerradas
	 */
	@Override
	@Transactional
	public int closeIdleOrders() {
		int encerradas = 0;

		for (Workshop workshop : workshopRepository.findByActiveTrueAndAutoCloseOpenOrdersHoursNotNull()) {
			Integer horas = workshop.getAutoCloseOpenOrdersHours();
			if (horas == null || horas < 1) {
				continue;
			}

			LocalDateTime limite = LocalDateTime.now().minusHours(horas);
			List<WorkOrder> paradas = workOrderRepository.findIdleOpenOrders(workshop.getId(), limite);

			for (WorkOrder order : paradas) {
				applyCancellation(order,
						"Encerrada automaticamente: aberta e sem movimentacao por mais de " + horas + "h.");
				encerradas++;
			}

			if (!paradas.isEmpty()) {
				log.info("Encerramento automatico: oficina={} limite={}h ordens={}", workshop.getId(), horas,
						paradas.size());
			}
		}

		return encerradas;
	}

	/**
	 * O cancelamento propriamente dito.
	 *
	 * <p>
	 * Extraido para que o caminho do balcao e o da rotina automatica apliquem a
	 * mesma regra. Duas copias divergiriam no dia em que uma delas mudasse, e o
	 * sintoma seria estoque errado -- o tipo de erro que nao da mensagem, so um
	 * numero que nao bate.
	 * </p>
	 */
	private void applyCancellation(WorkOrder order, String note) {
		applyTransition(order, WorkOrderStatus.CANCELED, note);
		order.setCanceledAt(LocalDateTime.now());

		// Cancelou, as pecas voltam para a prateleira.
		for (WorkOrderItem item : order.getItems()) {
			releaseStock(item, order);
		}

		workOrderRepository.save(order);

		log.info("OS cancelada: os={} numero={}", order.getId(), order.getNumber());
	}

	// ---------------------------------------------------------- orcamento
	@Override
	@Transactional
	public WorkOrderResponse submitQuote(UUID id, String email) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		if (order.getItems().isEmpty()) {
			throw new BusinessException("Adicione ao menos um servico ou peca antes de enviar o orcamento.");
		}

		order.setQuoteStatus(QuoteStatus.PENDING);
		// O motivo da recusa anterior nao vale para a proposta nova.
		order.setQuoteRejectionReason(null);
		if (order.getStatus() != WorkOrderStatus.WAITING_APPROVAL) {
			applyTransition(order, WorkOrderStatus.WAITING_APPROVAL, "Orcamento enviado para aprovacao");
		}
		workOrderRepository.save(order);

		sendQuoteEmail(order, email);

		log.info("Orcamento enviado: os={} total={}", id, order.getTotalAmount());
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse resendQuote(UUID id, String email) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		if (order.getQuoteStatus() != QuoteStatus.PENDING) {
			throw new BusinessException("So faz sentido reenviar um orcamento que esta aguardando resposta.");
		}

		String destino = sendQuoteEmail(order, email);
		if (destino == null) {
			throw new BusinessException(
					"Informe um e-mail para reenviar o orcamento, ou cadastre o e-mail do cliente.");
		}

		recordHistory(order, order.getStatus(), order.getStatus(), "Orcamento reenviado para " + destino);

		log.info("Orcamento reenviado: os={}", id);
		return respond(order);
	}

	/**
	 * Emite o link e dispara o e-mail com a proposta.
	 *
	 * <p>
	 * Endereco informado na hora tem prioridade sobre o do cadastro: o atendente
	 * pode estar enviando para o e-mail do filho, do escritorio ou de quem de
	 * fato decide. O cadastro do cliente nao e alterado por isso -- quem o envio
	 * atingiu fica registrado no proprio token.
	 * </p>
	 *
	 * <p>
	 * Sem nenhum endereco o orcamento continua valendo: ele fica aguardando
	 * aprovacao e a gerencia registra a decisao no sistema. E o caso do cliente
	 * que responde por telefone ou esta no balcao.
	 * </p>
	 *
	 * @return o endereco usado, ou {@code null} quando nao havia para onde enviar
	 */
	private String sendQuoteEmail(WorkOrder order, String email) {
		String destino = firstFilled(email, order.getCustomer().getEmail());
		if (destino == null) {
			log.info("Orcamento sem e-mail de destino: os={}. Decisao ficara a cargo da gerencia.", order.getId());
			return null;
		}

		String rawToken = quoteTokenIssuer.issue(order, destino);

		Map<String, Object> variables = new LinkedHashMap<>();
		variables.put("customerName", order.getCustomer().getName());
		variables.put("workshopName", order.getWorkshop().getTradeName());
		variables.put("orderNumber", order.getNumber());
		variables.put("vehiclePlate", order.getVehicle().getPlate());
		variables.put("vehicleDescription", order.getVehicle().getBrand() + " " + order.getVehicle().getModel());
		variables.put("items", quoteEmailItems(order));
		variables.put("servicesTotal", money(order.getServicesTotal()));
		variables.put("partsTotal", money(order.getPartsTotal()));
		variables.put("discount", isPositive(order.getDiscount()) ? money(order.getDiscount()) : null);
		variables.put("totalAmount", money(order.getTotalAmount()));
		variables.put("quoteLink", quoteTokenIssuer.buildLink(rawToken));
		variables.put("validityHours", quoteTokenIssuer.getValidityHours());

		emailService.sendQuoteToCustomer(destino,
				"Orcamento do seu veiculo - OS " + order.getNumber() + " - " + order.getWorkshop().getTradeName(),
				variables);

		return destino;
	}

	/** Itens ja formatados: o template de e-mail nao deve saber de locale. */
	private List<Map<String, Object>> quoteEmailItems(WorkOrder order) {
		List<Map<String, Object>> items = new ArrayList<>();
		for (WorkOrderItem item : order.getItems()) {
			Map<String, Object> linha = new LinkedHashMap<>();
			linha.put("description", item.getDescription());
			linha.put("typeLabel", item.getType().getLabel());
			linha.put("quantity", item.getQuantity());
			linha.put("total", money(item.getTotalPrice()));
			items.add(linha);
		}
		return items;
	}

	private static String money(BigDecimal value) {
		return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
				.format(value == null ? BigDecimal.ZERO : value);
	}

	private static boolean isPositive(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) > 0;
	}

	private static String firstFilled(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}

	@Override
	@Transactional
	public WorkOrderResponse approveQuote(UUID id) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		if (order.getQuoteStatus() != QuoteStatus.PENDING) {
			throw new BusinessException("Nao ha orcamento aguardando aprovacao nesta ordem de servico.");
		}

		applyApproval(order, "Orcamento aprovado, registrado pela oficina");

		log.info("Orcamento aprovado: os={} total={}", id, order.getTotalAmount());
		return respond(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse rejectQuote(UUID id, NoteRequest request) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		if (order.getQuoteStatus() != QuoteStatus.PENDING) {
			throw new BusinessException("Nao ha orcamento aguardando aprovacao nesta ordem de servico.");
		}

		applyRejection(order, noteOr(request, "Orcamento recusado, registrado pela oficina"),
				request == null ? null : request.note());

		log.info("Orcamento recusado: os={}", id);
		return respond(order);
	}

	// ------------------------------------------- decisao do proprio cliente
	/**
	 * O caminho do cliente nao passa por {@code loadDetailed}.
	 *
	 * <p>
	 * Aquele metodo escopa por oficina a partir do usuario autenticado, e aqui
	 * nao ha usuario nenhum. Quem autoriza e o token: ele aponta para uma unica
	 * ordem e vale uma vez so. Por isso a ordem vem do proprio token, e nao de
	 * um id recebido na URL -- o cliente nunca informa qual ordem quer ver.
	 * </p>
	 */
	@Override
	@Transactional(readOnly = true)
	public PublicQuoteResponse findQuoteByToken(String token) {
		return PublicQuoteResponse.from(requireUsableToken(token));
	}

	@Override
	@Transactional
	public PublicQuoteResponse approveQuoteByToken(String token) {
		QuoteApprovalToken approval = requireUsableToken(token);
		WorkOrder order = approval.getWorkOrder();
		requirePendingQuote(order);

		applyApproval(order, "Orcamento aprovado pelo cliente pelo link enviado por e-mail");
		quoteTokenIssuer.markDecided(approval, "APPROVED");
		// Evento proprio: a tela mostra um aviso destacado, e nao apenas
		// recarrega em silencio como faz numa mudanca qualquer.
		realtimeNotifier.quoteDecided(order.getWorkshop().getId(), order.getId(), order.getNumber(), true);

		log.info("Orcamento aprovado pelo cliente: os={}", order.getId());
		return PublicQuoteResponse.from(approval);
	}

	@Override
	@Transactional
	public PublicQuoteResponse rejectQuoteByToken(String token, String note) {
		QuoteApprovalToken approval = requireUsableToken(token);
		WorkOrder order = approval.getWorkOrder();
		requirePendingQuote(order);

		String motivo = note == null || note.isBlank()
				? "Orcamento recusado pelo cliente pelo link enviado por e-mail"
				: "Recusado pelo cliente: " + note.trim();
		applyRejection(order, motivo, note);
		quoteTokenIssuer.markDecided(approval, "REJECTED");
		realtimeNotifier.quoteDecided(order.getWorkshop().getId(), order.getId(), order.getNumber(), false);

		log.info("Orcamento recusado pelo cliente: os={}", order.getId());
		return PublicQuoteResponse.from(approval);
	}

	/**
	 * Link invalido, expirado ou ja usado recebe sempre a mesma resposta.
	 *
	 * <p>
	 * Distinguir "nao existe" de "expirou" transformaria a rota publica em um
	 * verificador de tokens validos.
	 * </p>
	 */
	private QuoteApprovalToken requireUsableToken(String token) {
		return quoteTokenIssuer.findUsable(token)
				.orElseThrow(() -> new BusinessException(
						"Este link nao vale mais. Ele expira em pouco tempo e so pode ser usado uma vez. "
								+ "Entre em contato com a oficina para receber um novo."));
	}

	private void requirePendingQuote(WorkOrder order) {
		requireNotFinal(order);
		if (order.getQuoteStatus() != QuoteStatus.PENDING) {
			throw new BusinessException("Este orcamento ja foi respondido.");
		}
	}

	/**
	 * Aprovacao, venha ela do cliente ou da oficina.
	 *
	 * <p>
	 * Os dois caminhos passam por aqui de proposito. Se cada um aplicasse a
	 * regra por conta propria, bastaria uma correcao em um deles para as duas
	 * versoes divergirem -- e a diferenca so apareceria muito depois, num
	 * orcamento aprovado pela metade.
	 * </p>
	 */
	private void applyApproval(WorkOrder order, String note) {
		order.setQuoteStatus(QuoteStatus.APPROVED);
		order.setQuoteRejectionReason(null);
		order.setApprovedAt(LocalDateTime.now());
		forceStatus(order, WorkOrderStatus.APPROVED, note);
		workOrderRepository.save(order);
	}

	/**
	 * Recusa, venha ela do cliente ou da oficina.
	 *
	 * @param historyNote texto que entra na linha do tempo
	 * @param reason      motivo dito pelo cliente, guardado junto do orcamento
	 *                    para a oficina saber o que ajustar sem precisar
	 *                    procurar no historico
	 */
	private void applyRejection(WorkOrder order, String historyNote, String reason) {
		order.setQuoteStatus(QuoteStatus.REJECTED);
		order.setQuoteRejectionReason(blankToNull(reason));
		// Volta para aberta em vez de cancelar: a oficina ainda pode revisar e
		// reapresentar o orcamento, e cancelar continua sendo uma acao explicita.
		forceStatus(order, WorkOrderStatus.OPEN, historyNote);
		workOrderRepository.save(order);
	}

	@Override
	@Transactional
	public WorkOrderResponse reopenQuote(UUID id, NoteRequest request) {
		WorkOrder order = loadDetailed(id);
		requireNotFinal(order);

		if (order.getQuoteStatus() != QuoteStatus.APPROVED) {
			throw new BusinessException("So faz sentido reabrir um orcamento que ja foi aprovado.");
		}

		order.setQuoteStatus(QuoteStatus.PENDING);
		order.setApprovedAt(null);
		forceStatus(order, WorkOrderStatus.WAITING_APPROVAL,
				noteOr(request, "Orcamento reaberto para ajuste; aguarda nova aprovacao"));
		workOrderRepository.save(order);

		log.info("Orcamento reaberto: os={}", id);
		return respond(order);
	}

	// ---------------------------------------------------- regras internas
	/** Monta o item resolvendo catalogo e estoque, e ja reserva a peca. */
	private WorkOrderItem buildItem(WorkOrderItemRequest request, WorkOrder order, UUID workshopId) {
		WorkOrderItem item = new WorkOrderItem();
		item.setType(request.type());
		item.setQuantity(request.quantity());

		String description = blankToNull(request.description());
		BigDecimal unitPrice = request.unitPrice();

		if (request.type() == ItemType.PART) {
			if (request.partId() == null) {
				throw new BusinessException("Informe a peca do estoque para um item do tipo peca.");
			}
			Part part = partRepository.findByIdAndWorkshopId(request.partId(), workshopId)
					.orElseThrow(() -> ResourceNotFoundException.of("Peca"));

			item.setPart(part);
			description = description == null ? part.getName() : description;
			unitPrice = unitPrice == null ? part.getSalePrice() : unitPrice;

			// Reserva imediata: a peca sai do estoque ao entrar na OS.
			stockService.register(part, StockMovementType.OUTBOUND, request.quantity(),
					"Uso na OS #" + order.getNumber(), order);

		} else if (request.serviceCatalogId() != null) {
			ServiceCatalog service = serviceCatalogRepository
					.findByIdAndWorkshopId(request.serviceCatalogId(), workshopId)
					.orElseThrow(() -> ResourceNotFoundException.of("Servico"));

			item.setServiceCatalog(service);
			description = description == null ? service.getName() : description;
			unitPrice = unitPrice == null ? service.getDefaultPrice() : unitPrice;
		}

		if (description == null) {
			throw new BusinessException("Informe a descricao do item.");
		}

		item.setDescription(description);
		item.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
		item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())));
		return item;
	}

	/** Devolve ao estoque a peca reservada por um item. */
	private void releaseStock(WorkOrderItem item, WorkOrder order) {
		if (item.getType() == ItemType.PART && item.getPart() != null) {
			stockService.register(item.getPart(), StockMovementType.INBOUND, item.getQuantity(),
					"Devolucao da OS #" + order.getNumber(), order);
		}
	}

	private void recalculateTotals(WorkOrder order) {
		BigDecimal parts = BigDecimal.ZERO;
		BigDecimal services = BigDecimal.ZERO;

		for (WorkOrderItem item : order.getItems()) {
			if (item.getType() == ItemType.PART) {
				parts = parts.add(item.getTotalPrice());
			} else {
				services = services.add(item.getTotalPrice());
			}
		}

		BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
		BigDecimal total = parts.add(services).subtract(discount);

		if (total.signum() < 0) {
			throw new BusinessException("O desconto nao pode ser maior que o valor total da ordem de servico.");
		}

		order.setPartsTotal(parts);
		order.setServicesTotal(services);
		order.setTotalAmount(total);
	}

	/** Mudanca de status validada pela maquina de estados. */
	private void applyTransition(WorkOrder order, WorkOrderStatus target, String note) {
		WorkOrderStatus current = order.getStatus();

		if (current == target) {
			throw new BusinessException("A ordem de servico ja esta com o status \"" + target.getLabel() + "\".");
		}
		if (!current.canTransitionTo(target)) {
			throw new BusinessException("Nao e possivel mudar de \"" + current.getLabel() + "\" para \""
					+ target.getLabel() + "\".");
		}

		order.setStatus(target);
		recordHistory(order, current, target, note);
	}

	/**
	 * Mudanca de status disparada por uma acao de negocio, como aprovar orcamento.
	 *
	 * <p>
	 * A maquina de estados existe para barrar mudancas manuais fora de ordem. As
	 * acoes de orcamento tem transicao propria e bem definida, entao passam por
	 * aqui, mas continuam registrando historico.
	 * </p>
	 */
	private void forceStatus(WorkOrder order, WorkOrderStatus target, String note) {
		WorkOrderStatus current = order.getStatus();
		if (current == target) {
			recordHistory(order, current, target, note);
			return;
		}
		order.setStatus(target);
		recordHistory(order, current, target, note);
	}

	/**
	 * Registra o evento e avisa as telas abertas.
	 *
	 * <p>
	 * O aviso sai daqui de proposito: <em>todo</em> movimento da ordem passa por
	 * este metodo -- mudanca de etapa, decisao de orcamento, entrega,
	 * cancelamento, reenvio. Espalhar a chamada pelos vinte pontos que alteram a
	 * OS garantiria que alguem esqueceria de um, e a tela ficaria parada sem
	 * explicacao justamente no caso menos testado.
	 * </p>
	 */
	private void recordHistory(WorkOrder order, WorkOrderStatus from, WorkOrderStatus to, String note) {
		WorkOrderStatusHistory history = new WorkOrderStatusHistory();
		history.setWorkOrder(order);
		history.setFromStatus(from);
		history.setToStatus(to);
		history.setChangedBy(currentUser.find().map(principal -> principal.getUser()).orElse(null));
		history.setNote(note);
		statusHistoryRepository.save(history);

		realtimeNotifier.workOrderChanged(order.getWorkshop().getId(), order.getId(), order.getNumber());
	}

	private void requireNotFinal(WorkOrder order) {
		if (order.getStatus().isFinal()) {
			throw new BusinessException("Esta ordem de servico esta " + order.getStatus().getLabel().toLowerCase()
					+ " e nao pode mais ser alterada.");
		}
	}

	/** Itens so mudam com a OS aberta e com o orcamento ainda nao aprovado. */
	private void requireItemsUnlocked(WorkOrder order) {
		requireNotFinal(order);

		if (order.getStatus() == WorkOrderStatus.FINISHED) {
			throw new BusinessException(
					"A ordem esta finalizada. Reabra a execucao antes de alterar servicos ou pecas.");
		}
		if (order.getQuoteStatus() == QuoteStatus.APPROVED) {
			throw new BusinessException(
					"O orcamento ja foi aprovado pelo cliente. Reabra o orcamento para alterar os itens.");
		}
	}

	private boolean isEditable(WorkOrder order) {
		return !order.getStatus().isFinal() && order.getStatus() != WorkOrderStatus.FINISHED
				&& order.getQuoteStatus() != QuoteStatus.APPROVED;
	}

	private WorkOrderItem findItem(WorkOrder order, UUID itemId) {
		return order.getItems().stream().filter(item -> itemId.equals(item.getId())).findFirst()
				.orElseThrow(() -> ResourceNotFoundException.of("Item da ordem de servico"));
	}

	private Employee resolveMechanic(UUID mechanicId, UUID workshopId) {
		if (mechanicId == null) {
			return null;
		}
		return employeeRepository.findByIdAndWorkshopId(mechanicId, workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Funcionario"));
	}

	private WorkOrder load(UUID id) {
		return workOrderRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Ordem de servico"));
	}

	private WorkOrder loadDetailed(UUID id) {
		return workOrderRepository.findDetailedByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Ordem de servico"));
	}

	private WorkOrderResponse respond(WorkOrder order) {
		return WorkOrderResponse.from(order, isEditable(order));
	}

	private String noteOr(NoteRequest request, String fallback) {
		return request == null || blankToNull(request.note()) == null ? fallback : request.note().trim();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}

	private LocalDateTime startOfDay(LocalDate date) {
		return date == null ? null : date.atStartOfDay();
	}

	private LocalDateTime endOfDay(LocalDate date) {
		return date == null ? null : date.atTime(LocalTime.MAX);
	}
}
