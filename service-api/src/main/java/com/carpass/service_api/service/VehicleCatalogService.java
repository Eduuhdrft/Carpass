package com.carpass.service_api.service;

import java.util.List;

import com.carpass.service_api.domain.enums.VehicleKind;
import com.carpass.service_api.web.dto.catalog.CatalogOption;
import com.carpass.service_api.web.dto.catalog.VehicleCatalogDetail;

/**
 * Catalogo de marcas, modelos e versoes da tabela FIPE.
 *
 * <p>
 * Serve para padronizar o cadastro: sem ele, o mesmo carro entra como "Gol",
 * "gol g6" e "VW Gol 1.6", e nenhuma busca por modelo funciona depois.
 * </p>
 */
public interface VehicleCatalogService {

	List<CatalogOption> brands(VehicleKind kind);

	List<CatalogOption> models(VehicleKind kind, String brandCode);

	/** Anos disponiveis para um modelo. O rotulo ja traz o combustivel. */
	List<CatalogOption> years(VehicleKind kind, String brandCode, String modelCode);

	/** O veiculo escolhido, com codigo FIPE e combustivel. */
	VehicleCatalogDetail detail(VehicleKind kind, String brandCode, String modelCode, String yearCode);
}
