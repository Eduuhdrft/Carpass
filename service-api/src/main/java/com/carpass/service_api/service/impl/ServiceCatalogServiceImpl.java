package com.carpass.service_api.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.model.ServiceCatalog;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.ServiceCatalogRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.ServiceCatalogService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogRequest;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogResponse;

/**
 * Regras do catalogo de servicos.
 *
 * <p>
 * O nome e unico dentro da oficina para que a lista de selecao na ordem de
 * servico nao fique com entradas ambiguas.
 * </p>
 */
@Service
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

	private static final Logger log = LoggerFactory.getLogger(ServiceCatalogServiceImpl.class);

	private final ServiceCatalogRepository serviceCatalogRepository;
	private final WorkshopRepository workshopRepository;
	private final CurrentUser currentUser;

	public ServiceCatalogServiceImpl(ServiceCatalogRepository serviceCatalogRepository,
			WorkshopRepository workshopRepository, CurrentUser currentUser) {
		this.serviceCatalogRepository = serviceCatalogRepository;
		this.workshopRepository = workshopRepository;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ServiceCatalogResponse> search(String term, Boolean active, Pageable pageable) {
		return PageResponse.from(
				serviceCatalogRepository.search(currentUser.requireWorkshopId(), normalize(term), active, pageable),
				ServiceCatalogResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public ServiceCatalogResponse findById(UUID id) {
		return ServiceCatalogResponse.from(load(id));
	}

	@Override
	@Transactional
	public ServiceCatalogResponse create(ServiceCatalogRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		String name = request.name().trim();

		if (serviceCatalogRepository.existsByWorkshopIdAndNameIgnoreCase(workshopId, name)) {
			throw new ConflictException("Ja existe um servico com este nome no catalogo.");
		}

		ServiceCatalog service = new ServiceCatalog();
		service.setWorkshop(workshopRepository.getReferenceById(workshopId));
		service.setActive(true);
		apply(request, service, name);
		serviceCatalogRepository.save(service);

		log.info("Servico criado no catalogo: servico={} oficina={}", service.getId(), workshopId);
		return ServiceCatalogResponse.from(service);
	}

	@Override
	@Transactional
	public ServiceCatalogResponse update(UUID id, ServiceCatalogRequest request) {
		ServiceCatalog service = load(id);
		String name = request.name().trim();

		if (serviceCatalogRepository.existsByWorkshopIdAndNameIgnoreCaseAndIdNot(currentUser.requireWorkshopId(), name,
				id)) {
			throw new ConflictException("Ja existe outro servico com este nome no catalogo.");
		}

		apply(request, service, name);
		serviceCatalogRepository.save(service);

		log.info("Servico do catalogo atualizado: servico={}", id);
		return ServiceCatalogResponse.from(service);
	}

	@Override
	@Transactional
	public ServiceCatalogResponse changeStatus(UUID id, boolean active) {
		ServiceCatalog service = load(id);
		service.setActive(active);
		serviceCatalogRepository.save(service);

		log.info("Servico do catalogo {}: servico={}", active ? "reativado" : "desativado", id);
		return ServiceCatalogResponse.from(service);
	}

	// -------------------------------------------------------------- apoio
	private void apply(ServiceCatalogRequest request, ServiceCatalog service, String name) {
		service.setName(name);
		service.setDescription(request.description());
		service.setDefaultPrice(request.defaultPrice());
		service.setEstimatedMinutes(request.estimatedMinutes());
	}

	private ServiceCatalog load(UUID id) {
		return serviceCatalogRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Servico"));
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}
}
