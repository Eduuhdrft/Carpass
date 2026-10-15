package com.carpass.service_api.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.carpass.service_api.config.SecurityProperties;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Implementacao do {@link JwtService} usando a biblioteca java-jwt (auth0).
 */
@Service
public class JwtServiceImpl implements JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);
	private static final String ISSUER = "carpass-api";
	private static final String CLAIM_USER = "uid";
	private static final String CLAIM_WORKSHOP = "wid";

	private final Algorithm algorithm;
	private final JWTVerifier verifier;
	private final long expirationSeconds;

	public JwtServiceImpl(SecurityProperties properties) {
		this.algorithm = Algorithm.HMAC256(properties.getJwtSecret());
		this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
		this.expirationSeconds = properties.getJwtExpirationSeconds();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * O papel <strong>nao</strong> entra mais no token. Ele mudou de lugar: vive
	 * no vinculo, que pode ser rebaixado a qualquer momento. Um papel congelado
	 * aqui continuaria valendo ate o token expirar, e rebaixar um gerente a
	 * atendente nao teria efeito nenhum pelo resto do dia. Agora o papel e lido
	 * do banco a cada requisicao, junto com a checagem de que o vinculo ainda
	 * existe.
	 * </p>
	 */
	@Override
	public String generateToken(User user, WorkshopMember membership) {
		Instant now = Instant.now();
		return JWT.create()
				.withIssuer(ISSUER)
				.withSubject(user.getEmail())
				.withClaim(CLAIM_USER, user.getId().toString())
				.withClaim(CLAIM_WORKSHOP,
						membership == null ? null : membership.getWorkshop().getId().toString())
				.withIssuedAt(now)
				.withExpiresAt(now.plus(expirationSeconds, ChronoUnit.SECONDS))
				.sign(algorithm);
	}

	@Override
	public Optional<TokenClaims> extractClaims(String token) {
		try {
			DecodedJWT decoded = verifier.verify(token);
			String subject = decoded.getSubject();
			if (subject == null) {
				return Optional.empty();
			}
			return Optional.of(new TokenClaims(subject, readWorkshopId(decoded)));
		} catch (JWTVerificationException ex) {
			log.debug("Token rejeitado: {}", ex.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Le a oficina do token tolerando lixo.
	 *
	 * <p>
	 * Um {@code wid} que nao seja UUID e token adulterado ou de uma versao
	 * anterior do formato. Tratar como "sem oficina" derruba a requisicao para
	 * 403 no primeiro acesso a dado operacional, que e o desfecho certo -- deixar
	 * a excecao subir viraria 500 e esconderia a causa.
	 * </p>
	 */
	private UUID readWorkshopId(DecodedJWT decoded) {
		String raw = decoded.getClaim(CLAIM_WORKSHOP).asString();
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException ex) {
			log.debug("Token com oficina invalida no claim {}: {}", CLAIM_WORKSHOP, raw);
			return null;
		}
	}

	@Override
	public long getExpirationSeconds() {
		return expirationSeconds;
	}
}
