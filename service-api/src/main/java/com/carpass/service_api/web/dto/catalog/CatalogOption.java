package com.carpass.service_api.web.dto.catalog;

/**
 * Uma opcao da tabela FIPE: marca, modelo ou ano.
 *
 * <p>
 * Os tres niveis tem a mesma forma -- um codigo que o proximo passo usa e um
 * nome que a pessoa le -- entao um record so atende os tres. Tres records
 * identicos com nomes diferentes seriam tres lugares para manter a mesma coisa.
 * </p>
 *
 * @param code  codigo da FIPE; opaco para o sistema, so serve para a proxima
 *              chamada
 * @param label como aparece na tela ("ARGO DRIVE 1.3 8V Flex")
 */
public record CatalogOption(String code, String label) {
}
