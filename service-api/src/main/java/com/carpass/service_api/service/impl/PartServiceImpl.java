package com.carpass.service_api.service.impl;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartType;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.PartRepository;
import com.carpass.service_api.repository.PartStockMovementRepository;
import com.carpass.service_api.repository.PartTypeRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.repository.projection.ManufacturerSummary;
import com.carpass.service_api.repository.spec.PartSpecifications;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.PartService;
import com.carpass.service_api.service.StockService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.part.PartFilter;
import com.carpass.service_api.web.dto.part.PartManufacturerResponse;
import com.carpass.service_api.web.dto.part.PartRequest;
import com.carpass.service_api.web.dto.part.PartResponse;
import com.carpass.service_api.web.dto.part.StockMovementRequest;
import com.carpass.service_api.web.dto.part.StockMovementResponse;

/**
 * Regras de cadastro de pecas.
 *
 * <p>
 * O saldo em estoque nunca e escrito diretamente por este servico: toda
 * alteracao passa pelo {@link StockService}, o que garante que exista uma
 * movimentacao registrada para cada mudanca de saldo.
 * </p>
 */
@Service
public class PartServiceImpl implements PartService {

	private static final Logger log = LoggerFactory.getLogger(PartServiceImpl.class);

	private final PartRepository partRepository;
	private final PartTypeRepository partTypeRepository;
	private final PartStockMovementRepository movementRepository;
	private final WorkshopRepository workshopRepository;
	private final StockService stockService;
	private final CurrentUser currentUser;

	public PartServiceImpl(PartRepository partRepository, PartTypeRepository partTypeRepository,
			PartStockMovementRepository movementRepository, WorkshopRepository workshopRepository,
			StockService stockService, CurrentUser currentUser) {
		this.partRepository = partRepository;
		this.partTypeRepository = partTypeRepository;
		this.movementRepository = movementRepository;
		this.workshopRepository = workshopRepository;
		this.stockService = stockService;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PartResponse> search(PartFilter filter, Pageable pageable) {
		Specification<Part> spec = PartSpecifications.filter(currentUser.requireWorkshopId(),
				normalize(filter.term()), filter.active(), filter.lowStockOnly(), filter.partTypeId(),
				normalize(filter.manufacturer()));

		return PageResponse.from(partRepository.findAll(spec, pageable), PartResponse::from);
	}

	/**
	 * A escolha entre as duas consultas de marca fica aqui, e nao no repositorio.
	 *
	 * <p>
	 * Sao duas porque parametro opcional em JPQL vira placeholder sem tipo e o
	 * PostgreSQL recusa a instrucao inteira -- a mesma armadilha que ja derrubou a
	 * busca de ordens de servico. O repositorio nao precisa conhecer o conceito de
	 * "filtro ausente"; ele so tem duas perguntas bem definidas.
	 * </p>
	 */
	@Override
	@Transactional(readOnly = true)
	public List<PartManufacturerResponse> findManufacturers(UUID partTypeId) {
		UUID workshopId = currentUser.requireWorkshopId();

		List<ManufacturerSummary> marcas = partTypeId == null
				? partRepository.findManufacturers(workshopId)
				: partRepository.findManufacturersByType(workshopId, partTypeId);

		return marcas.stream().map(PartManufacturerResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PartResponse findById(UUID id) {
		return PartResponse.from(load(id));
	}

	@Override
	@Transactional
	public PartResponse create(PartRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		String code = blankToNull(request.code());

		if (code != null && partRepository.existsByWorkshopIdAndCodeIgnoreCase(workshopId, code)) {
			throw new ConflictException("Ja existe uma peca com este codigo nesta oficina.");
		}

		Part part = new Part();
		part.setWorkshop(workshopRepository.getReferenceById(workshopId));
		part.setActive(true);
		part.setStockQuantity(0);
		apply(request, part, code);
		partRepository.save(part);

		// O estoque inicial entra como movimentacao, para nascer ja rastreavel.
		int initialStock = request.initialStock() == null ? 0 : request.initialStock();
		if (initialStock > 0) {
			stockService.register(part, StockMovementType.INBOUND, initialStock, "Estoque inicial do cadastro", null);
		}

		log.info("Peca criada: peca={} oficina={}", part.getId(), workshopId);
		return PartResponse.from(part);
	}

	@Override
	@Transactional
	public PartResponse update(UUID id, PartRequest request) {
		Part part = load(id);
		String code = blankToNull(request.code());

		if (code != null
				&& partRepository.existsByWorkshopIdAndCodeIgnoreCaseAndIdNot(currentUser.requireWorkshopId(), code, id)) {
			throw new ConflictException("Ja existe outra peca com este codigo nesta oficina.");
		}

		// initialStock e ignorado na edicao: saldo so muda por movimentacao.
		apply(request, part, code);
		partRepository.save(part);

		log.info("Peca atualizada: peca={}", id);
		return PartResponse.from(part);
	}

	@Override
	@Transactional
	public PartResponse changeStatus(UUID id, boolean active) {
		Part part = load(id);
		part.setActive(active);
		partRepository.save(part);

		log.info("Peca {}: peca={}", active ? "reativada" : "desativada", id);
		return PartResponse.from(part);
	}

	@Override
	@Transactional
	public PartResponse registerStockMovement(UUID id, StockMovementRequest request) {
		Part part = load(id);
		stockService.register(part, request.type(), request.quantity(), request.reason(), null);
		return PartResponse.from(part);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> findStockMovements(UUID id, Pageable pageable) {
		UUID workshopId = currentUser.requireWorkshopId();
		// Confere que a peca e desta oficina antes de expor o historico.
		load(id);

		return PageResponse.from(
				movementRepository.findByWorkshopIdAndPartIdOrderByCreatedAtDesc(workshopId, id, pageable),
				StockMovementResponse::from);
	}

	// -------------------------------------------------------------- apoio
	private void apply(PartRequest request, Part part, String code) {
		part.setCode(code);
		part.setName(request.name().trim());
		part.setManufacturer(blankToNull(request.manufacturer()));
		part.setPartType(resolveType(request.partTypeId()));
		part.setCostPrice(request.costPrice());
		part.setSalePrice(request.salePrice());
		part.setMinimumStock(request.minimumStock());
	}

	private Part load(UUID id) {
		return partRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Peca"));
	}

	/**
	 * Resolve o tipo conferindo a oficina.
	 *
	 * <p>
	 * O id vem da requisicao, entao <strong>tem</strong> que ser buscado por
	 * {@code findByIdAndWorkshopId}. Um {@code getReferenceById} aqui aceitaria o
	 * id de um tipo de outra oficina sem reclamar, e a peca nasceria apontando
	 * para fora -- e o vazamento so apareceria no dia em que alguem lesse o nome
	 * do tipo. Tipo de outra oficina responde 404, como todo recurso alheio.
	 * </p>
	 */
	private PartType resolveType(UUID partTypeId) {
		if (partTypeId == null) {
			return null;
		}

		return partTypeRepository.findByIdAndWorkshopId(partTypeId, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Tipo de peca"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}
}
