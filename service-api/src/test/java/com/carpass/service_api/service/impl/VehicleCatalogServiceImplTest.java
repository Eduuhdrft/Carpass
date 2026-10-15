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
import com.carpass.service_api.domain.enums.FuelType;
import com.carpass.service_api.domain.enums.VehicleKind;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.web.dto.catalog.VehicleCatalogDetail;

/**
 * Catalogo FIPE.
 *
 * <p>
 * O que estes testes travam: o caminho certo para cada tipo de veiculo -- a
 * FIPE sao tres tabelas separadas --, a traducao do combustivel e o
 * comportamento quando a API externa nao responde.
 * </p>
 */
class VehicleCatalogServiceImplTest {

	private static final String BASE = "https://fipe.teste/api/v1";

	private MockRestServiceServer servidor;
	private VehicleCatalogServiceImpl service;

	@BeforeEach
	void setUp() {
		IntegrationProperties propriedades = new IntegrationProperties();
		propriedades.setFipeUrl(BASE);

		RestClient.Builder builder = RestClient.builder();
		servidor = MockRestServiceServer.bindTo(builder).build();
		service = new VehicleCatalogServiceImpl(builder.build(), propriedades);
	}

	@Test
	@DisplayName("cada tipo de veiculo consulta a sua tabela")
	void eachKindHasItsOwnPath() {
		servidor.expect(requestTo(BASE + "/motos/marcas"))
				.andRespond(withSuccess("[{\"codigo\":\"77\",\"nome\":\"Honda\"}]", MediaType.APPLICATION_JSON));

		// Marca de carro nao existe na tabela de motos: errar o caminho devolveria
		// uma lista plausivel e completamente errada.
		assertThat(service.brands(VehicleKind.MOTORCYCLE))
				.singleElement()
				.satisfies(opcao -> {
					assertThat(opcao.code()).isEqualTo("77");
					assertThat(opcao.label()).isEqualTo("Honda");
				});
		servidor.verify();
	}

	@Test
	@DisplayName("a lista de modelos vem dentro de um objeto, nao solta")
	void modelsComeWrapped() {
		servidor.expect(requestTo(BASE + "/carros/marcas/21/modelos"))
				.andRespond(withSuccess("""
						{"modelos":[{"codigo":4828,"nome":"ARGO DRIVE 1.3 8V Flex"}],
						 "anos":[{"codigo":"2023-1","nome":"2023 Gasolina"}]}
						""", MediaType.APPLICATION_JSON));

		assertThat(service.models(VehicleKind.CAR, "21"))
				.singleElement()
				.satisfies(opcao -> assertThat(opcao.label()).isEqualTo("ARGO DRIVE 1.3 8V Flex"));
		servidor.verify();
	}

	@Test
	@DisplayName("o detalhe traz codigo FIPE e combustivel traduzido")
	void detailBringsFipeCodeAndFuel() {
		darDetalhe("2023 Gasolina", 2023);

		VehicleCatalogDetail detalhe = service.detail(VehicleKind.CAR, "21", "4828", "2023-1");

		assertThat(detalhe.fipeCode()).isEqualTo("001004-9");
		assertThat(detalhe.model()).isEqualTo("ARGO DRIVE 1.3 8V Flex");
		assertThat(detalhe.modelYear()).isEqualTo(2023);
		assertThat(detalhe.fuelType()).isEqualTo(FuelType.GASOLINE);
		// O texto original e preservado: se um dia a traducao falhar, a informacao
		// nao se perde.
		assertThat(detalhe.fuelLabel()).isEqualTo("2023 Gasolina");
	}

	@Test
	@DisplayName("combustivel desconhecido nao vira chute")
	void unknownFuelIsNull() {
		darDetalhe("Hidrogenio", 2030);

		VehicleCatalogDetail detalhe = service.detail(VehicleKind.CAR, "21", "4828", "2030-1");

		// Escolher o mais parecido gravaria um dado errado com cara de certo.
		assertThat(detalhe.fuelType()).isNull();
		assertThat(detalhe.fuelLabel()).isEqualTo("Hidrogenio");
	}

	@Test
	@DisplayName("o ano 32000 da FIPE significa zero km, e nao um ano")
	void zeroKilometerIsNotAYear() {
		darDetalhe("Flex", 32000);

		assertThat(service.detail(VehicleKind.CAR, "21", "4828", "32000-1").modelYear()).isNull();
	}

	@Test
	@DisplayName("codigo com caractere estranho nao entra na URL")
	void rejectsCodesThatCouldEscapeTheUrl() {
		// Sem esta guarda, um parametro com barra apontaria a chamada para outro
		// caminho do servico externo.
		assertThatThrownBy(() -> service.models(VehicleKind.CAR, "../../outro"))
				.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> service.years(VehicleKind.CAR, "21", "4828/anos"))
				.isInstanceOf(BusinessException.class);
		servidor.verify();
	}

	@Test
	@DisplayName("FIPE fora do ar vira mensagem util, e nao erro interno")
	void unavailableBecomesBusinessError() {
		servidor.expect(requestTo(BASE + "/carros/marcas")).andRespond(withServerError());

		assertThatThrownBy(() -> service.brands(VehicleKind.CAR))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("manualmente");
	}

	@Test
	@DisplayName("a segunda consulta das mesmas marcas nao sai da maquina")
	void cachesLists() {
		servidor.expect(requestTo(BASE + "/carros/marcas"))
				.andRespond(withSuccess("[{\"codigo\":\"21\",\"nome\":\"Fiat\"}]", MediaType.APPLICATION_JSON));

		service.brands(VehicleKind.CAR);
		service.brands(VehicleKind.CAR);
		service.brands(VehicleKind.CAR);

		// Uma expectativa so: uma segunda requisicao faria o mock falhar.
		servidor.verify();
	}

	private void darDetalhe(String combustivel, int anoModelo) {
		servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/carros/marcas/21/modelos/4828/anos/")))
				.andRespond(withSuccess("""
						{"Valor":"R$ 75.000,00","Marca":"Fiat","Modelo":"ARGO DRIVE 1.3 8V Flex",
						 "AnoModelo":%d,"Combustivel":"%s","CodigoFipe":"001004-9",
						 "MesReferencia":"agosto de 2026 "}
						""".formatted(anoModelo, combustivel), MediaType.APPLICATION_JSON));
	}
}
