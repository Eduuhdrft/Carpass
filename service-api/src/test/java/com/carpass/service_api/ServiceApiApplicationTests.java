package com.carpass.service_api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.web.controller.AuthController;
import com.carpass.service_api.web.controller.MeController;
import com.carpass.service_api.web.controller.WorkshopController;

/**
 * Verifica que o contexto sobe e que os componentes principais foram
 * registrados. Roda contra banco em memoria, sem depender do Postgres local.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceApiApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("o contexto da aplicacao sobe com todos os beans obrigatorios")
	void contextLoads() {
		assertThat(context.getBean(AuthController.class)).isNotNull();
		assertThat(context.getBean(MeController.class)).isNotNull();
		assertThat(context.getBean(WorkshopController.class)).isNotNull();
	}
}
