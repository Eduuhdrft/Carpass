package com.carpass.service_api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Positive;

/**
 * Configuracoes de seguranca da aplicacao, sempre vindas de variaveis de
 * ambiente. Nenhum segredo fica no codigo-fonte.
 *
 * <p>
 * Prefixo: {@code carpass.security}.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "carpass.security")
public class SecurityProperties {

	/** Tamanho minimo aceitavel para o segredo do JWT (256 bits). */
	private static final int MIN_SECRET_LENGTH = 32;

	/** Valor propositalmente inseguro usado apenas para detectar falta de configuracao. */
	private static final String INSECURE_PLACEHOLDER = "change-me";

	/**
	 * Segredo de assinatura do JWT. Sem valor padrao de proposito: a validacao em
	 * {@link #validate()} produz uma mensagem explicando o que configurar.
	 */
	private String jwtSecret;

	@Positive
	private long jwtExpirationSeconds = 28800L; // 8 horas

	/** Origens autorizadas a chamar a API. Deve ser explicita, sem curinga. */
	private List<String> allowedOrigins = List.of("http://localhost:4200");

	/** Minutos de validade do link de redefinicao de senha. */
	@Positive
	private long passwordResetTokenMinutes = 30L;

	/** URL base do frontend, usada para montar o link de redefinicao de senha. */
	private String frontendBaseUrl = "http://localhost:4200";

	@PostConstruct
	void validate() {
		if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.startsWith(INSECURE_PLACEHOLDER)
				|| jwtSecret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException("""

					===============================================================
					 CarPass nao pode subir: CARPASS_JWT_SECRET ausente ou fraco.
					===============================================================

					Como resolver, em dois passos:

					  1) Na pasta backend/service-api, crie o arquivo .env a
					     partir do exemplo:

					       cp .env.example .env

					  2) Gere um segredo aleatorio e cole em CARPASS_JWT_SECRET
					     dentro do .env (minimo de %d caracteres):

					       openssl rand -base64 48

					O arquivo .env e lido automaticamente ao subir a aplicacao,
					seja pela IDE, pelo gradlew bootRun ou pelo jar, e esta no
					.gitignore. Variaveis de ambiente reais do sistema, quando
					existirem, tem prioridade sobre ele.
					""".formatted(MIN_SECRET_LENGTH));
		}
		if (allowedOrigins.contains("*")) {
			throw new IllegalStateException(
					"carpass.security.allowed-origins nao pode conter '*' porque a API usa credenciais.");
		}
	}

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public long getJwtExpirationSeconds() {
		return jwtExpirationSeconds;
	}

	public void setJwtExpirationSeconds(long jwtExpirationSeconds) {
		this.jwtExpirationSeconds = jwtExpirationSeconds;
	}

	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public long getPasswordResetTokenMinutes() {
		return passwordResetTokenMinutes;
	}

	public void setPasswordResetTokenMinutes(long passwordResetTokenMinutes) {
		this.passwordResetTokenMinutes = passwordResetTokenMinutes;
	}

	public String getFrontendBaseUrl() {
		return frontendBaseUrl;
	}

	public void setFrontendBaseUrl(String frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}
}
