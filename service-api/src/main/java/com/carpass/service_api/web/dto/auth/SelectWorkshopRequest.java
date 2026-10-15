package com.carpass.service_api.web.dto.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * A oficina em que a pessoa quer operar.
 *
 * <p>
 * O id vindo daqui nao autoriza nada por si so: o servico confere que existe um
 * vinculo ativo entre esta pessoa e esta oficina antes de emitir qualquer token.
 * Sem essa conferencia, trocar o id no corpo seria entrar na oficina de outro.
 * </p>
 */
public record SelectWorkshopRequest(

		@NotNull(message = "Informe a oficina")
		UUID workshopId) {
}
