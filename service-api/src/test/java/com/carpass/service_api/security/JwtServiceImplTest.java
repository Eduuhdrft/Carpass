package com.carpass.service_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carpass.service_api.config.SecurityProperties;
import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Testes do ciclo de emissao e verificacao do token.
 */
class JwtServiceImplTest {

	private JwtServiceImpl jwtService;
	private User user;
	private WorkshopMember membership;
	private UUID workshopId;

	@BeforeEach
	void setUp() {
		SecurityProperties properties = new SecurityProperties();
		properties.setJwtSecret("segredo-de-teste-com-mais-de-32-caracteres-aqui");
		properties.setJwtExpirationSeconds(3600);
		jwtService = new JwtServiceImpl(properties);

		user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("dono@oficina.com");

		workshopId = UUID.randomUUID();
		Workshop workshop = new Workshop();
		workshop.setId(workshopId);
		workshop.setTradeName("Oficina de Teste");

		membership = new WorkshopMember();
		membership.setUser(user);
		membership.setWorkshop(workshop);
		membership.setRole(Role.OWNER);
		membership.setActive(true);
	}

	@Test
	@DisplayName("o token gerado carrega o e-mail do usuario como subject")
	void generatesTokenWithSubject() {
		String token = jwtService.generateToken(user, membership);

		assertThat(jwtService.extractClaims(token)).get()
				.extracting(JwtService.TokenClaims::email)
				.isEqualTo("dono@oficina.com");
	}

	@Test
	@DisplayName("o token carrega a oficina escolhida")
	void generatesTokenWithWorkshop() {
		String token = jwtService.generateToken(user, membership);

		assertThat(jwtService.extractClaims(token)).get()
				.extracting(JwtService.TokenClaims::workshopId)
				.isEqualTo(workshopId);
	}

	@Test
	@DisplayName("sem vinculo, o token sai sem oficina")
	void generatesTokenWithoutWorkshop() {
		// E o caso de quem foi demitido e ainda nao criou a propria oficina: entra,
		// mas nao alcanca dado operacional de oficina nenhuma.
		String token = jwtService.generateToken(user, null);

		assertThat(jwtService.extractClaims(token)).get()
				.extracting(JwtService.TokenClaims::workshopId)
				.isNull();
	}

	@Test
	@DisplayName("o papel nao viaja no token")
	void doesNotCarryRole() {
		// O papel mudou de lugar: vive no vinculo, que pode ser rebaixado a
		// qualquer momento. Congelado no token, rebaixar um gerente a atendente
		// nao teria efeito nenhum pelo resto do dia.
		String token = jwtService.generateToken(user, membership);
		String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

		assertThat(payload).doesNotContain("role").doesNotContain("OWNER");
	}

	@Test
	@DisplayName("token adulterado e rejeitado sem lancar excecao")
	void rejectsTamperedToken() {
		String token = jwtService.generateToken(user, membership);
		String tampered = token.substring(0, token.length() - 3) + "abc";

		assertThat(jwtService.extractClaims(tampered)).isEmpty();
	}

	@Test
	@DisplayName("texto que nao e um token e rejeitado sem lancar excecao")
	void rejectsGarbage() {
		assertThat(jwtService.extractClaims("nao-e-um-token")).isEqualTo(Optional.empty());
	}

	@Test
	@DisplayName("token assinado com outro segredo e rejeitado")
	void rejectsTokenFromAnotherSecret() {
		SecurityProperties other = new SecurityProperties();
		other.setJwtSecret("outro-segredo-completamente-diferente-com-32-chars");
		String foreignToken = new JwtServiceImpl(other).generateToken(user, membership);

		assertThat(jwtService.extractClaims(foreignToken)).isEmpty();
	}
}
