package com.carpass.service_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da API do CarPass.
 *
 * <p>
 * {@code @EnableScheduling} liga a rotina que encerra ordens paradas. Ela roda
 * dentro da propria aplicacao, sem servico externo -- adequado para uma
 * instancia. Com mais de uma, as duas fariam a mesma varredura no mesmo minuto;
 * ver a nota em {@code IdleWorkOrderCloser}.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class ServiceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApiApplication.class, args);
	}
}
