package com.carpass.service_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Integracoes com servicos publicos externos.
 *
 * <p>
 * Prefixo: {@code carpass.integration}. Nenhuma delas exige chave -- sao APIs
 * abertas -- mas os enderecos ficam em configuracao mesmo assim: no dia em que
 * uma sair do ar ou mudar de dominio, trocar uma variavel de ambiente e mais
 * rapido que publicar uma versao.
 * </p>
 *
 * <h2>Por que ha tempo limite curto</h2>
 *
 * <p>
 * Estas chamadas acontecem enquanto alguem espera olhando para um formulario.
 * Um servico externo lento nao pode virar uma tela travada: passado o prazo, a
 * busca desiste e a pessoa preenche a mao, que e como ela fazia antes.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "carpass.integration")
public class IntegrationProperties {

	/** Fonte principal de CEP. Agrega varios provedores e cai para o proximo sozinha. */
	@NotBlank
	private String cepPrimaryUrl = "https://brasilapi.com.br/api/cep/v2";

	/** Reserva de CEP, usada quando a principal falha ou demora. */
	@NotBlank
	private String cepFallbackUrl = "https://viacep.com.br/ws";

	/** Tabela FIPE. E a unica gratuita com a cadeia completa marca -> modelo -> ano. */
	@NotBlank
	private String fipeUrl = "https://parallelum.com.br/fipe/api/v1";

	/** Prazo de cada tentativa. Curto de proposito: alguem esta esperando. */
	private Duration timeout = Duration.ofSeconds(4);

	/**
	 * Por quanto tempo a resposta fica guardada.
	 *
	 * <p>
	 * CEP praticamente nunca muda e a tabela FIPE muda uma vez por mes. O cache
	 * existe menos por velocidade e mais por educacao: sao servicos publicos e
	 * gratuitos, e repetir a mesma pergunta a cada digito seria abuso.
	 * </p>
	 */
	private Duration cacheTtl = Duration.ofHours(12);

	/** Teto de entradas do cache, para ele nunca virar um vazamento de memoria. */
	private int cacheMaxEntries = 2000;

	public String getCepPrimaryUrl() {
		return cepPrimaryUrl;
	}

	public void setCepPrimaryUrl(String cepPrimaryUrl) {
		this.cepPrimaryUrl = cepPrimaryUrl;
	}

	public String getCepFallbackUrl() {
		return cepFallbackUrl;
	}

	public void setCepFallbackUrl(String cepFallbackUrl) {
		this.cepFallbackUrl = cepFallbackUrl;
	}

	public String getFipeUrl() {
		return fipeUrl;
	}

	public void setFipeUrl(String fipeUrl) {
		this.fipeUrl = fipeUrl;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Duration getCacheTtl() {
		return cacheTtl;
	}

	public void setCacheTtl(Duration cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

	public int getCacheMaxEntries() {
		return cacheMaxEntries;
	}

	public void setCacheMaxEntries(int cacheMaxEntries) {
		this.cacheMaxEntries = cacheMaxEntries;
	}
}
