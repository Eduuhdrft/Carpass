package com.carpass.service_api.web.dto.part;

import com.carpass.service_api.repository.projection.ManufacturerSummary;

/**
 * Uma marca disponivel no estoque da oficina.
 *
 * <h2>Por que a lista vem do servidor, e nao do que a tela ja carregou</h2>
 *
 * <p>
 * A listagem e paginada: montar o filtro com as marcas da pagina atual ofereceria
 * so as vinte primeiras pecas e esconderia o resto -- e o filtro que esconde
 * opcao valida e pior que nao ter filtro. Aqui a lista sai do estoque inteiro.
 * </p>
 *
 * <p>
 * Vem com contagem e saldo de proposito: ao perguntar "quais marcas de oleo de
 * motor eu tenho?", saber que de uma sobrou 1 litro e de outra sobraram 24 e
 * exatamente a parte util da resposta.
 * </p>
 *
 * @param name          marca como esta cadastrada nas pecas
 * @param partCount     quantas pecas ativas sao dessa marca
 * @param stockQuantity soma do saldo dessas pecas
 */
public record PartManufacturerResponse(String name, long partCount, long stockQuantity) {

	public static PartManufacturerResponse from(ManufacturerSummary summary) {
		return new PartManufacturerResponse(summary.manufacturer(), summary.partCount(), summary.stockQuantity());
	}
}
