package com.carpass.service_api.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.DefaultPartTypes;
import com.carpass.service_api.domain.model.PartType;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.PartTypeRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.repository.spec.PartTypeSpecifications;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.PartTypeService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.parttype.PartTypeRequest;
import com.carpass.service_api.web.dto.parttype.PartTypeResponse;

/**
 * Regras do cadastro de tipos de peca.
 *
 * <p>
 * O nome e unico dentro da oficina, ignorando a caixa. Sem isso o cadastro
 * aceitaria "Vela" e "vela" lado a lado e o filtro voltaria a ter o problema que
 * o tipo veio resolver -- so que agora com aparencia de estrutura.
 * </p>
 */
@Service
public class PartTypeServiceImpl implements PartTypeService {

	private static final Logger log = LoggerFactory.getLogger(PartTypeServiceImpl.class);

	private final PartTypeRepository partTypeRepository;
	private final WorkshopRepository workshopRepository;
	private final CurrentUser currentUser;

	public PartTypeServiceImpl(PartTypeRepository partTypeRepository, WorkshopRepository workshopRepository,
			CurrentUser currentUser) {
		this.partTypeRepository = partTypeRepository;
		this.workshopRepository = workshopRepository;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PartTypeResponse> search(String term, Boolean active, Pageable pageable) {
		UUID workshopId = currentUser.requireWorkshopId();
		Map<UUID, Long> counts = loadPartCounts(workshopId);

		return PageResponse.from(
				partTypeRepository.findAll(PartTypeSpecifications.filter(workshopId, normalize(term), active),
						pageable),
				type -> PartTypeResponse.from(type, counts.getOrDefault(type.getId(), 0L)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartTypeResponse> listActive() {
		return partTypeRepository.findByWorkshopIdAndActiveTrueOrderByNameAsc(currentUser.requireWorkshopId()).stream()
				.map(PartTypeResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PartTypeResponse findById(UUID id) {
		return PartTypeResponse.from(load(id));
	}

	@Override
	@Transactional
	public PartTypeResponse create(PartTypeRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		String name = request.name().trim();

		if (partTypeRepository.existsByWorkshopIdAndNameIgnoreCase(workshopId, name)) {
			throw new ConflictException("Ja existe um tipo de peca com este nome.");
		}

		PartType type = new PartType();
		type.setWorkshop(workshopRepository.getReferenceById(workshopId));
		type.setActive(true);
		apply(request, type, name);
		partTypeRepository.save(type);

		log.info("Tipo de peca criado: tipo={} oficina={}", type.getId(), workshopId);
		return PartTypeResponse.from(type);
	}

	@Override
	@Transactional
	public PartTypeResponse update(UUID id, PartTypeRequest request) {
		PartType type = load(id);
		String name = request.name().trim();

		if (partTypeRepository.existsByWorkshopIdAndNameIgnoreCaseAndIdNot(currentUser.requireWorkshopId(), name, id)) {
			throw new ConflictException("Ja existe outro tipo de peca com este nome.");
		}

		apply(request, type, name);
		partTypeRepository.save(type);

		log.info("Tipo de peca atualizado: tipo={}", id);
		return PartTypeResponse.from(type);
	}

	/**
	 * Desativa ou reativa.
	 *
	 * <p>
	 * Nunca apaga, mesmo sem peca nenhuma usando: a peca guarda o tipo por
	 * referencia, e apagar quebraria o cadastro de qualquer peca que tenha usado
	 * aquele tipo um dia. Desativado ele some dos seletores e continua descrevendo
	 * o que ja esta la.
	 * </p>
	 */
	@Override
	@Transactional
	public PartTypeResponse changeStatus(UUID id, boolean active) {
		PartType type = load(id);
		type.setActive(active);
		partTypeRepository.save(type);

		log.info("Tipo de peca {}: tipo={}", active ? "reativado" : "desativado", id);
		return PartTypeResponse.from(type, countFor(id));
	}

	@Override
	@Transactional
	public void seedDefaults(Workshop workshop) {
		List<PartType> types = new ArrayList<>();

		for (DefaultPartTypes.Seed seed : DefaultPartTypes.all()) {
			PartType type = new PartType();
			type.setWorkshop(workshop);
			type.setName(seed.name());
			type.setDescription(seed.description());
			type.setActive(true);
			types.add(type);
		}

		partTypeRepository.saveAll(types);
		log.info("Tipos de peca padrao criados: oficina={} quantidade={}", workshop.getId(), types.size());
	}

	// -------------------------------------------------------------- apoio
	private void apply(PartTypeRequest request, PartType type, String name) {
		type.setName(name);
		type.setDescription(blankToNull(request.description()));
	}

	private PartType load(UUID id) {
		return partTypeRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Tipo de peca"));
	}

	/**
	 * Contagem de pecas por tipo em uma consulta so.
	 *
	 * <p>
	 * A alternativa seria contar por linha da listagem, e uma pagina de vinte
	 * tipos viraria vinte e uma consultas.
	 * </p>
	 */
	private Map<UUID, Long> loadPartCounts(UUID workshopId) {
		Map<UUID, Long> counts = new HashMap<>();
		for (Object[] row : partTypeRepository.countActivePartsByType(workshopId)) {
			counts.put((UUID) row[0], ((Number) row[1]).longValue());
		}
		return counts;
	}

	private long countFor(UUID typeId) {
		return loadPartCounts(currentUser.requireWorkshopId()).getOrDefault(typeId, 0L);
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
