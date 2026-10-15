package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.carpass.service_api.config.IntegrationProperties;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.web.dto.catalog.AddressLookupResponse;

/**
 * Busca de CEP.
 *
 * <p>
 * As chamadas sao interceptadas pelo {@code MockRestServiceServer}, que ja vem
 * no {@code spring-boot-starter-test}. Bater na BrasilAPI durante o build
 * tornaria a suite dependente da internet e do humor de um servico de terceiro
 * -- e ela precisa dizer se o <em>nosso</em> codigo esta certo, nao se o deles
 * esta no ar.
 * </p>
 */
class AddressLookupServiceImplTest {

	private static final String PRINCIPAL = "https://fonte-principal.teste/cep";
	private static final String RESERVA = "https://fonte-reserva.teste/ws";

	private MockRestServiceServer servidor;
	private AddressLookupServiceImpl service;

	@BeforeEach
	void setUp() {
		IntegrationProperties propriedades = new IntegrationProperties();
		propriedades.setCepPrimaryUrl(PRINCIPAL);
		propriedades.setCepFallbackUrl(RESERVA);

		RestClient.Builder builder = RestClient.builder();
		servidor = MockRestServiceServer.bindTo(builder).build();
		service = new AddressLookupServiceImpl(builder.build(), propriedades);
	}

	@Test
	@DisplayName("CEP com menos de oito digitos nem chega a consultar")
	void rejectsShortZipCode() {
		assertThatThrownBy(() -> service.findByZipCode("0131")).isInstanceOf(BusinessException.class);
		// Nenhuma expectativa registrada: se tivesse saido requisicao, o verify
		// abaixo acusaria.
		servidor.verify();
	}

	@Test
	@DisplayName("a mascara e ignorada: 01310-100 e 01310100 sao o mesmo CEP")
	void normalizesTheMask() {
		darRespostaDaPrincipal();

		AddressLookupResponse resposta = service.findByZipCode("01310-100");

		assertThat(resposta.zipCode()).isEqualTo("01310100");
		assertThat(resposta.street()).isEqualTo("Avenida Paulista");
		assertThat(resposta.city()).isEqualTo("Sao Paulo");
		assertThat(resposta.state()).isEqualTo("SP");
		servidor.verify();
	}

	@Test
	@DisplayName("quando a principal falha, a reserva responde")
	void fallsBackToViaCep() {
		servidor.expect(requestTo(PRINCIPAL + "/01310100")).andRespond(withServerError());
		servidor.expect(requestTo(RESERVA + "/01310100/json/"))
				.andRespond(withSuccess("""
						{"cep":"01310-100","logradouro":"Avenida Paulista","bairro":"Bela Vista",
						 "localidade":"Sao Paulo","uf":"SP"}
						""", MediaType.APPLICATION_JSON));

		AddressLookupResponse resposta = service.findByZipCode("01310100");

		assertThat(resposta.neighborhood()).isEqualTo("Bela Vista");
		servidor.verify();
	}

	@Test
	@DisplayName("o ViaCEP responde 200 com erro, e isso nao e um endereco")
	void viaCepErrorFlagIsNotAnAddress() {
		servidor.expect(requestTo(PRINCIPAL + "/00000000")).andRespond(withServerError());
		// Sucesso HTTP nao significa que achou: e a armadilha classica do ViaCEP.
		servidor.expect(requestTo(RESERVA + "/00000000/json/"))
				.andRespond(withSuccess("{\"erro\":\"true\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> service.findByZipCode("00000000"))
				.isInstanceOf(ResourceNotFoundException.class);
		servidor.verify();
	}

	@Test
	@DisplayName("as duas fontes fora do ar viram 404, e nao 500")
	void bothSourcesDownMeansNotFound() {
		servidor.expect(requestTo(PRINCIPAL + "/01310100")).andRespond(withServerError());
		servidor.expect(requestTo(RESERVA + "/01310100/json/")).andRespond(withServerError());

		// A pessoa preenche a mao. Um servico de terceiro fora do ar nao pode
		// virar erro interno do CarPass.
		assertThatThrownBy(() -> service.findByZipCode("01310100"))
				.isInstanceOf(ResourceNotFoundException.class);
		servidor.verify();
	}

	@Test
	@DisplayName("a segunda consulta do mesmo CEP nao sai da maquina")
	void cachesTheAnswer() {
		// Uma unica expectativa: se sair uma segunda requisicao, o mock falha.
		darRespostaDaPrincipal();

		service.findByZipCode("01310100");
		service.findByZipCode("01310-100");
		service.findByZipCode("01310100");

		servidor.verify();
	}

	private void darRespostaDaPrincipal() {
		servidor.expect(requestTo(PRINCIPAL + "/01310100"))
				.andRespond(withSuccess("""
						{"cep":"01310100","state":"SP","city":"Sao Paulo","neighborhood":"Bela Vista",
						 "street":"Avenida Paulista"}
						""", MediaType.APPLICATION_JSON));
	}
}
