package com.carpass.service_api.web.dto.catalog;

import com.carpass.service_api.domain.enums.FuelType;

/**
 * O veiculo escolhido na tabela FIPE, pronto para preencher o cadastro.
 *
 * <h2>Por que o codigo FIPE vem junto</h2>
 *
 * <p>
 * Ele identifica aquele modelo e versao de forma estavel, independente de como
 * o nome for escrito. Guardado no veiculo, permite mais tarde consultar valor de
 * mercado sem pedir nada de novo a pessoa -- e custa uma coluna.
 * </p>
 *
 * @param fipeCode  codigo no formato "001004-9"
 * @param brand     marca como a FIPE escreve
 * @param model     modelo <strong>com a versao</strong>: a FIPE nao separa os
 *                  dois, e "ARGO DRIVE 1.3 8V Flex" e um valor unico
 * @param modelYear ano do modelo; 32000 na FIPE significa "zero km"
 * @param fuelType  combustivel ja traduzido para o enum do sistema; nulo quando
 *                  a FIPE usa um termo que nao temos
 * @param fuelLabel combustivel como a FIPE escreveu, para nao perder a
 *                  informacao quando a traducao falha
 */
public record VehicleCatalogDetail(String fipeCode, String brand, String model, Integer modelYear,
		FuelType fuelType, String fuelLabel) {
}
