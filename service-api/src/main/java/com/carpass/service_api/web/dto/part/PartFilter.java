package com.carpass.service_api.web.dto.part;

import java.util.UUID;

/**
 * Filtros da listagem de pecas.
 *
 * <p>
 * Agrupados num record em vez de seis parametros soltos: a assinatura de
 * {@code search} ja estava em quatro, e listas de parametros longas do mesmo
 * tipo sao onde nasce a troca silenciosa de argumento -- passar o termo de busca
 * na posicao da marca compila e devolve resultado errado.
 * </p>
 *
 * @param term         texto livre: nome, codigo ou fabricante
 * @param active       null traz ativas e inativas
 * @param lowStockOnly so o que esta no estoque minimo ou abaixo
 * @param partTypeId   o tipo escolhido no filtro
 * @param manufacturer a marca escolhida no filtro
 */
public record PartFilter(String term, Boolean active, boolean lowStockOnly, UUID partTypeId, String manufacturer) {
}
