package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.part.PartFilter;
import com.carpass.service_api.web.dto.part.PartManufacturerResponse;
import com.carpass.service_api.web.dto.part.PartRequest;
import com.carpass.service_api.web.dto.part.PartResponse;
import com.carpass.service_api.web.dto.part.StockMovementRequest;
import com.carpass.service_api.web.dto.part.StockMovementResponse;

/**
 * Pecas e estoque da oficina.
 */
public interface PartService {

	PageResponse<PartResponse> search(PartFilter filter, Pageable pageable);

	/**
	 * Marcas presentes no estoque, opcionalmente de um tipo so.
	 *
	 * <p>
	 * E a resposta para "quais marcas de oleo de motor eu tenho?" -- a pergunta
	 * que o balcao faz de verdade com o carro no elevador.
	 * </p>
	 *
	 * @param partTypeId null traz as marcas de todo o estoque
	 */
	List<PartManufacturerResponse> findManufacturers(UUID partTypeId);

	PartResponse findById(UUID id);

	PartResponse create(PartRequest request);

	PartResponse update(UUID id, PartRequest request);

	PartResponse changeStatus(UUID id, boolean active);

	/** Registra entrada, saida ou ajuste manual de estoque. */
	PartResponse registerStockMovement(UUID id, StockMovementRequest request);

	PageResponse<StockMovementResponse> findStockMovements(UUID id, Pageable pageable);
}
