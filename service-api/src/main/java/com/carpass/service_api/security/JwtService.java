package com.carpass.service_api.security;

import java.util.Optional;
import java.util.UUID;

import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Emissao e verificacao dos tokens de acesso.
 */
public interface JwtService {

	/**
	 * O que um token valido carrega.
	 *
	 * <p>
	 * A oficina e apenas <strong>a escolha</strong> que a pessoa fez ao entrar --
	 * nunca uma autorizacao por si so. Quem confere se aquele vinculo existe e
	 * continua ativo e o filtro, a cada requisicao, contra o banco. Um token
	 * assinado com a oficina certa deixa de autenticar no instante em que o
	 * vinculo cai.
	 * </p>
	 *
	 * @param email      o subject do token
	 * @param workshopId a oficina escolhida, ou nulo quando a pessoa entrou sem
	 *                   vinculo
	 */
	record TokenClaims(String email, UUID workshopId) {
	}

	/**
	 * Gera um token para a pessoa operando na oficina do vinculo informado.
	 *
	 * @param membership vinculo escolhido, ou nulo para quem entra sem oficina
	 */
	String generateToken(User user, WorkshopMember membership);

	/**
	 * Valida o token e devolve o que ele carrega.
	 *
	 * @return os dados do token, ou {@link Optional#empty()} se ele for invalido,
	 *         expirado ou emitido por outra aplicacao
	 */
	Optional<TokenClaims> extractClaims(String token);

	/** Tempo de vida do token, em segundos. Usado na resposta de login. */
	long getExpirationSeconds();
}
