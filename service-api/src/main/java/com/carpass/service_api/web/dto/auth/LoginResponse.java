package com.carpass.service_api.web.dto.auth;

import java.util.List;

import com.carpass.service_api.web.dto.user.UserResponse;

/**
 * Resposta do login. O token vai no corpo para ser guardado pelo cliente e
 * enviado no cabecalho Authorization das proximas requisicoes.
 *
 * <h2>Tres desfechos possiveis</h2>
 *
 * <ul>
 * <li><strong>Um vinculo</strong> -- o caso comum. O token ja vem escopado
 * naquela oficina e a pessoa entra direto.</li>
 * <li><strong>Varios vinculos</strong> -- o token vem sem oficina e
 * {@code workshops} traz as opcoes. O cliente escolhe e chama
 * {@code POST /auth/session/workshop} para receber o token escopado.</li>
 * <li><strong>Nenhum vinculo</strong> -- quem foi demitido e ainda nao tem
 * oficina propria. Token sem oficina e lista vazia: entra, nao ve dado de
 * oficina nenhuma, e pode criar a dele.</li>
 * </ul>
 *
 * <p>
 * Os tres devolvem 200 com um token de verdade, de proposito. A alternativa
 * seria um estado intermediario "meio autenticado", com um token especial que
 * so serve para escolher -- mais peca para manter e mais lugar para errar, sem
 * ganho: um token sem oficina ja nao alcanca nada operacional, porque nenhuma
 * expressao de papel casa com papel nulo.
 * </p>
 *
 * @param accessToken token JWT assinado
 * @param tokenType   sempre "Bearer"
 * @param expiresIn   validade do token em segundos
 * @param user        dados basicos do usuario autenticado
 * @param workshops   oficinas em que a pessoa pode entrar
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user,
		List<WorkshopOption> workshops) {

	public static LoginResponse of(String accessToken, long expiresIn, UserResponse user,
			List<WorkshopOption> workshops) {
		return new LoginResponse(accessToken, "Bearer", expiresIn, user, workshops);
	}
}
