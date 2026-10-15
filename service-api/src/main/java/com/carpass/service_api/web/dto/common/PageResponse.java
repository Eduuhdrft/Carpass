package com.carpass.service_api.web.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Envelope de paginacao estavel, para que o contrato da API nao dependa da
 * serializacao interna do {@link Page} do Spring Data.
 *
 * @param content       itens da pagina atual
 * @param page          indice da pagina, comecando em zero
 * @param size          tamanho da pagina
 * @param totalElements total de registros que atendem ao filtro
 * @param totalPages    total de paginas
 * @param first         true se e a primeira pagina
 * @param last          true se e a ultima pagina
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean first,
		boolean last) {

	public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
		return new PageResponse<>(source.getContent().stream().map(mapper).toList(), source.getNumber(),
				source.getSize(), source.getTotalElements(), source.getTotalPages(), source.isFirst(), source.isLast());
	}
}
