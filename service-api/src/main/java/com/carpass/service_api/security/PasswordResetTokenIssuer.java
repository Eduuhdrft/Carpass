package com.carpass.service_api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.carpass.service_api.config.SecurityProperties;
import com.carpass.service_api.domain.model.PasswordResetToken;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.repository.PasswordResetTokenRepository;

/**
 * Emissao e consumo dos tokens de definicao de senha.
 *
 * <p>
 * Serve tanto para "esqueci minha senha" quanto para o primeiro acesso de um
 * funcionario recem-cadastrado: os dois fluxos precisam exatamente da mesma
 * garantia, entao a logica mora em um lugar so.
 * </p>
 *
 * <p>
 * O valor puro do token existe apenas no e-mail enviado. O banco guarda somente
 * o hash SHA-256, de modo que um vazamento da tabela nao permite redefinir a
 * senha de ninguem.
 * </p>
 */
@Component
public class PasswordResetTokenIssuer {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final PasswordResetTokenRepository tokenRepository;
	private final SecurityProperties securityProperties;

	public PasswordResetTokenIssuer(PasswordResetTokenRepository tokenRepository,
			SecurityProperties securityProperties) {
		this.tokenRepository = tokenRepository;
		this.securityProperties = securityProperties;
	}

	/**
	 * Invalida os tokens em aberto do usuario e emite um novo.
	 *
	 * @return o token puro, que deve ser enviado por e-mail e nunca persistido
	 */
	public String issue(User user) {
		tokenRepository.invalidateOpenTokens(user.getId(), LocalDateTime.now());

		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setTokenHash(hash(rawToken));
		token.setExpiresAt(LocalDateTime.now().plusMinutes(securityProperties.getPasswordResetTokenMinutes()));
		tokenRepository.save(token);

		return rawToken;
	}

	/** Localiza um token ainda utilizavel a partir do valor puro. */
	public Optional<PasswordResetToken> findUsable(String rawToken) {
		return tokenRepository.findByTokenHash(hash(rawToken)).filter(PasswordResetToken::isUsable);
	}

	/** Marca o token como consumido. */
	public void markUsed(PasswordResetToken token) {
		token.setUsedAt(LocalDateTime.now());
		tokenRepository.save(token);
	}

	/** Invalida todos os tokens em aberto, usado quando a senha muda por outro caminho. */
	public void invalidateAll(User user) {
		tokenRepository.invalidateOpenTokens(user.getId(), LocalDateTime.now());
	}

	/** Monta o link que vai no e-mail. */
	public String buildLink(String rawToken) {
		return securityProperties.getFrontendBaseUrl() + "/definir-senha?token=" + rawToken;
	}

	public long getValidityMinutes() {
		return securityProperties.getPasswordResetTokenMinutes();
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
