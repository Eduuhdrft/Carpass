package com.carpass.service_api.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogRequest;
import com.carpass.service_api.web.dto.servicecatalog.ServiceCatalogResponse;

/**
 * Catalogo de servicos da oficina.
 */
public interface ServiceCatalogService {

	PageResponse<ServiceCatalogResponse> search(String term, Boolean active, Pageable pageable);

	ServiceCatalogResponse findById(UUID id);

	ServiceCatalogResponse create(ServiceCatalogRequest request);

	ServiceCatalogResponse update(UUID id, ServiceCatalogRequest request);

	ServiceCatalogResponse changeStatus(UUID id, boolean active);
}
