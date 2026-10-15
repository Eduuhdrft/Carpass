package com.carpass.service_api.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.carpass.service_api.config.IntegrationProperties;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.service.AddressLookupService;
import com.carpass.service_api.web.dto.catalog.AddressLookupResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Busca de CEP com duas fontes.
 *
 * <h2>Por que o servidor consulta, e nao o navegador</h2>
 *
 * <ul>
 * <li><strong>Cache.</strong> Os clientes de uma oficina moram, em boa parte, no
 * mesmo punhado de bairros. Guardar aqui responde a maioria das buscas sem sair
 * da maquina -- e sao servicos publicos e gratuitos: repetir a mesma pergunta a
 * cada digito seria abuso.</li>
 * <li><strong>Reserva.</strong> Trocar de provedor quando o primeiro falha e
 * decisao de servidor. No navegador, isso viraria regra espalhada na tela.</li>
 * <li><strong>Uma unica forma de resposta.</strong> BrasilAPI e ViaCEP devolvem
 * campos com nomes diferentes; a tela nao precisa saber de qual delas veio.</li>
 * </ul>
 *
 * <h2>O que acontece quando as duas falham</h2>
 *
 * <p>
 * A busca desiste e a pessoa preenche a mao, como fazia antes. Nenhum cadastro
 * fica bloqueado por causa de um servico de terceiro fora do ar -- e o erro nao
 * e guardado no cache, senao uma queda de dez segundos viraria doze horas de
 * "CEP nao encontrado".
 * </p>
 */
@Service
public class AddressLookupServiceImpl implements AddressLookupService {

	private static final Logger log = LoggerFactory.getLogger(AddressLookupServiceImpl.class);

	private final RestClient restClient;
	private final IntegrationProperties properties;
	private final ExternalCache<String, AddressLookupResponse> cache;

	public AddressLookupServiceImpl(@Qualifier("integrationRestClient") RestClient restClient,
			IntegrationProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
		this.cache = new ExternalCache<>(properties.getCacheTtl(), properties.getCacheMaxEntries());
	}

	@Override
	public AddressLookupResponse findByZipCode(String zipCode) {
		String digitos = onlyDigits(zipCode);

		if (digitos.length() != 8) {
			throw new BusinessException("Informe um CEP com 8 digitos.");
		}

		return cache.get(digitos, () -> buscar(digitos))
				.orElseThrow(() -> ResourceNotFoundException.of("CEP"));
	}

	/** Tenta a fonte principal e, se ela nao responder, a reserva. */
	private Optional<AddressLookupResponse> buscar(String cep) {
		Optional<AddressLookupResponse> principal = pelaBrasilApi(cep);
		if (principal.isPresent()) {
			return principal;
		}
		return peloViaCep(cep);
	}

	// ------------------------------------------------------- BrasilAPI
	private Optional<AddressLookupResponse> pelaBrasilApi(String cep) {
		try {
			BrasilApiCep corpo = restClient.get()
					.uri(properties.getCepPrimaryUrl() + "/" + cep)
					.retrieve()
					.body(BrasilApiCep.class);

			if (corpo == null || isBlank(corpo.city())) {
				return Optional.empty();
			}

			return Optional.of(new AddressLookupResponse(cep, safe(corpo.street()), safe(corpo.neighborhood()),
					safe(corpo.city()), safe(corpo.state())));
		} catch (Exception ex) {
			// CEP inexistente devolve 404 e cai aqui junto com falha de rede. Os dois
			// casos tem o mesmo tratamento: tentar a reserva.
			log.debug("Consulta de CEP na fonte principal falhou para {}: {}", cep, ex.getMessage());
			return Optional.empty();
		}
	}

	/*
	 * Estes dois records descrevem corpos de terceiros, que podem ganhar campos
	 * sem aviso. O ignoreUnknown e explicito -- o padrao do Spring Boot ja e
	 * esse, mas depender de configuracao global numa fronteira que nao
	 * controlamos e como deixar a porta encostada.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record BrasilApiCep(String cep, String state, String city, String neighborhood, String street) {
	}

	// ---------------------------------------------------------- ViaCEP
	private Optional<AddressLookupResponse> peloViaCep(String cep) {
		try {
			ViaCep corpo = restClient.get()
					.uri(properties.getCepFallbackUrl() + "/" + cep + "/json/")
					.retrieve()
					.body(ViaCep.class);

			// O ViaCEP responde 200 com {"erro": ...} para CEP inexistente -- ou
			// seja, sucesso HTTP nao significa que achou.
			if (corpo == null || corpo.temErro() || isBlank(corpo.localidade())) {
				return Optional.empty();
			}

			return Optional.of(new AddressLookupResponse(cep, safe(corpo.logradouro()), safe(corpo.bairro()),
					safe(corpo.localidade()), safe(corpo.uf())));
		} catch (Exception ex) {
			log.debug("Consulta de CEP na fonte reserva falhou para {}: {}", cep, ex.getMessage());
			return Optional.empty();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ViaCep(String cep, String logradouro, String complemento, String bairro, String localidade,
			String uf, String erro) {

		/*
		 * O campo `erro` ja veio como booleano e como texto em versoes diferentes
		 * do ViaCEP. Recebendo como String, o Jackson aceita os dois -- e a
		 * comparacao aqui nao precisa saber qual chegou.
		 */
		boolean temErro() {
			return erro != null && "true".equalsIgnoreCase(erro.trim());
		}
	}

	// ----------------------------------------------------------- apoio
	private String onlyDigits(String valor) {
		return valor == null ? "" : valor.replaceAll("\\D", "");
	}

	private String safe(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private boolean isBlank(String valor) {
		return valor == null || valor.isBlank();
	}
}
