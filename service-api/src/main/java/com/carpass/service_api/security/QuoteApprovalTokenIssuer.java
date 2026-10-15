package com.carpass.service_api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.carpass.service_api.config.SecurityProperties;
import com.carpass.service_api.domain.model.QuoteApprovalToken;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.repository.QuoteApprovalTokenRepository;

/**
 * Emissao e consumo dos links de aprovacao de orcamento.
 *
 * <p>
 * Mesma garantia do token de senha: 32 bytes aleatorios, so o hash SHA-256 no
 * banco, uso unico e validade curta. A diferenca e o dono -- aqui quem usa o
 * link e o cliente da oficina, que nao tem conta no sistema. O token e, ele
 * proprio, a autorizacao.
 * </p>
 */
@Component
public class QuoteApprovalTokenIssuer {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final QuoteApprovalTokenRepository tokenRepository;
	private final SecurityProperties securityProperties;
	private final long validityHours;

	public QuoteApprovalTokenIssuer(QuoteApprovalTokenRepository tokenRepository,
			SecurityProperties securityProperties,
			@Value("${carpass.quote.approval-token-hours:24}") long validityHours) {
		this.tokenRepository = tokenRepository;
		this.securityProperties = securityProperties;
		this.validityHours = validityHours;
	}

	/**
	 * Derruba os links abertos da ordem e emite um novo.
	 *
	 * @param sentTo endereco para onde a proposta sera enviada
	 * @return o token puro, que vai no e-mail e nunca e persistido
	 */
	public String issue(WorkOrder order, String sentTo) {
		tokenRepository.invalidateOpenTokens(order.getId(), LocalDateTime.now());

		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		QuoteApprovalToken token = new QuoteApprovalToken();
		token.setWorkOrder(order);
		token.setTokenHash(hash(rawToken));
		token.setSentTo(sentTo);
		token.setExpiresAt(LocalDateTime.now().plusHours(validityHours));
		tokenRepository.save(token);

		return rawToken;
	}

	/** Localiza um link ainda utilizavel a partir do valor puro. */
	public Optional<QuoteApprovalToken> findUsable(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return Optional.empty();
		}
		return tokenRepository.findByTokenHash(hash(rawToken)).filter(QuoteApprovalToken::isUsable);
	}

	/** Marca como consumido e registra a decisao tomada. */
	public void markDecided(QuoteApprovalToken token, String decision) {
		LocalDateTime now = LocalDateTime.now();
		token.setUsedAt(now);
		token.setDecision(decision);
		token.setDecidedAt(now);
		tokenRepository.save(token);
	}

	/** Monta o link que vai no e-mail. */
	public String buildLink(String rawToken) {
		return securityProperties.getFrontendBaseUrl() + "/orcamento/" + rawToken;
	}

	public long getValidityHours() {
		return validityHours;
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponivel nesta JVM", ex);
		}
	}
}
