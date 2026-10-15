package com.carpass.service_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP das integracoes externas.
 *
 * <h2>Por que um cliente proprio, com prazo</h2>
 *
 * <p>
 * O {@code RestClient} padrao do Spring nasce <strong>sem tempo limite</strong>.
 * Numa chamada que acontece enquanto alguem espera olhando para um formulario,
 * isso significa que um servico externo pendurado deixa a requisicao pendurada
 * junto -- e, com trafego, prende as threads do servidor uma a uma. O prazo aqui
 * e o que garante que a busca desiste antes de a pessoa desistir.
 * </p>
 */
@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationConfig {

	/**
	 * Cliente exclusivo das integracoes.
	 *
	 * <p>
	 * Separado de qualquer outro uso de HTTP na aplicacao de proposito: o prazo
	 * curto e adequado para consultar um CEP e seria curto demais para outras
	 * coisas.
	 * </p>
	 */
	@Bean
	public RestClient integrationRestClient(IntegrationProperties properties) {
		Duration prazo = properties.getTimeout();

		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(prazo);
		factory.setReadTimeout(prazo);

		return RestClient.builder()
				.requestFactory(factory)
				// Identificar-se e boa educacao com um servico publico e gratuito, e
				// ajuda o mantenedor a saber quem o usa.
				.defaultHeader("User-Agent", "CarPass/1.0 (+https://carpass.app)")
				.build();
	}
}
