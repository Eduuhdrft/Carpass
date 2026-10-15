package com.carpass.service_api.web.dto.workshop;

import jakarta.validation.constraints.NotNull;

/**
 * Se o gerente enxerga o financeiro da oficina.
 *
 * <p>
 * Endpoint proprio, e nao um campo do cadastro da oficina, por um motivo de
 * seguranca: o cadastro e editavel pelo gerente. Se a chave viajasse junto, o
 * gerente se concederia acesso ao financeiro salvando a propria tela de
 * cadastro. Aqui so o dono passa.
 * </p>
 */
public record FinanceAccessRequest(

		@NotNull(message = "Informe se o gerente pode ver o financeiro")
		Boolean managerCanSeeFinance) {
}
