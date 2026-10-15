package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.parttype.PartTypeRequest;
import com.carpass.service_api.web.dto.parttype.PartTypeResponse;

/**
 * Cadastro de tipos de peca.
 */
public interface PartTypeService {

	PageResponse<PartTypeResponse> search(String term, Boolean active, Pageable pageable);

	/** Lista enxuta e sem paginacao, para preencher seletor e filtro. */
	List<PartTypeResponse> listActive();

	PartTypeResponse findById(UUID id);

	PartTypeResponse create(PartTypeRequest request);

	PartTypeResponse update(UUID id, PartTypeRequest request);

	PartTypeResponse changeStatus(UUID id, boolean active);

	/**
	 * Cria a lista padrao para uma oficina recem-criada.
	 *
	 * <p>
	 * Chamado no cadastro da oficina, dentro da mesma transacao: cadastro vazio e
	 * cadastro que ninguem preenche.
	 * </p>
	 */
	void seedDefaults(Workshop workshop);
}
