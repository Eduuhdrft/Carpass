package com.carpass.service_api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A aplicacao precisa se recusar a subir com configuracao insegura. Estes testes
 * travam esse comportamento.
 */
class SecurityPropertiesTest {

	@Test
	@DisplayName("recusa segredo de JWT ausente")
	void rejectsMissingSecret() {
		SecurityProperties properties = new SecurityProperties();

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CARPASS_JWT_SECRET");
	}

	@Test
	@DisplayName("recusa segredo de JWT curto demais")
	void rejectsShortSecret() {
		SecurityProperties properties = new SecurityProperties();
		properties.setJwtSecret("curto");

		assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("recusa curinga na lista de origens permitidas")
	void rejectsWildcardOrigin() {
		SecurityProperties properties = new SecurityProperties();
		properties.setJwtSecret("segredo-de-teste-com-mais-de-32-caracteres-aqui");
		properties.setAllowedOrigins(List.of("*"));

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("allowed-origins");
	}

	@Test
	@DisplayName("aceita configuracao valida")
	void acceptsValidConfiguration() {
		SecurityProperties properties = new SecurityProperties();
		properties.setJwtSecret("segredo-de-teste-com-mais-de-32-caracteres-aqui");
		properties.setAllowedOrigins(List.of("http://localhost:4200"));

		assertThatCode(properties::validate).doesNotThrowAnyException();
	}
}
