package com.carpass.service_api.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.carpass.service_api.config.IntegrationProperties;
import com.carpass.service_api.domain.enums.FuelType;
import com.carpass.service_api.domain.enums.VehicleKind;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.service.VehicleCatalogService;
import com.carpass.service_api.web.dto.catalog.CatalogOption;
import com.carpass.service_api.web.dto.catalog.VehicleCatalogDetail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Catalogo de veiculos apoiado na tabela FIPE.
 *
 * <h2>Por que FIPE</h2>
 *
 * <p>
 * E a lista que o mercado brasileiro inteiro ja usa: seguradora, revenda e
 * oficina falam a mesma lingua. O nome do modelo ja embute a versao
 * ("ARGO DRIVE 1.3 8V Flex"), que e exatamente o nivel de detalhe que faltava, e
 * o codigo FIPE identifica aquele veiculo de forma estavel.
 * </p>
 *
 * <h2>O que ela nao cobre</h2>
 *
 * <p>
 * A FIPE lista o que tem valor de mercado publicado. Carro muito antigo,
 * importado raro ou adaptado pode nao estar la -- por isso o cadastro manual
 * continua existindo, e nao como plano B envergonhado: e o caminho normal para
 * uma parte real da clientela de oficina.
 * </p>
 *
 * <h2>Cache</h2>
 *
 * <p>
 * A lista de marcas muda uma vez por ano; a de modelos, quase nunca. Guardar
 * evita repetir a mesma pergunta a um servico publico e gratuito a cada vez que
 * alguem abre o formulario -- e faz o seletor abrir instantaneo depois da
 * primeira vez.
 * </p>
 */
@Service
public class VehicleCatalogServiceImpl implements VehicleCatalogService {

	private static final Logger log = LoggerFactory.getLogger(VehicleCatalogServiceImpl.class);

	/** A FIPE usa 32000 para "zero km". Nao e um ano. */
	private static final int ZERO_KM = 32000;

	private final RestClient restClient;
	private final IntegrationProperties properties;
	private final ExternalCache<String, List<CatalogOption>> optionsCache;
	private final ExternalCache<String, VehicleCatalogDetail> detailCache;

	public VehicleCatalogServiceImpl(@Qualifier("integrationRestClient") RestClient restClient,
			IntegrationProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
		this.optionsCache = new ExternalCache<>(properties.getCacheTtl(), properties.getCacheMaxEntries());
		this.detailCache = new ExternalCache<>(properties.getCacheTtl(), properties.getCacheMaxEntries());
	}

	@Override
	public List<CatalogOption> brands(VehicleKind kind) {
		String caminho = base(kind) + "/marcas";
		return listar("marcas:" + kind, caminho);
	}

	@Override
	public List<CatalogOption> models(VehicleKind kind, String brandCode) {
		String marca = exigir(brandCode, "Escolha a marca antes do modelo.");
		String caminho = base(kind) + "/marcas/" + marca + "/modelos";

		// Este endereco devolve um objeto com "modelos" e "anos" dentro, e nao uma
		// lista solta como os outros dois niveis.
		return optionsCache.get("modelos:" + kind + ":" + marca, () -> {
			try {
				FipeModelos corpo = restClient.get().uri(caminho).retrieve().body(FipeModelos.class);
				if (corpo == null || corpo.modelos() == null) {
					return Optional.empty();
				}
				return Optional.of(corpo.modelos().stream()
						.map(item -> new CatalogOption(String.valueOf(item.codigo()), item.nome()))
						.toList());
			} catch (Exception ex) {
				log.warn("Consulta de modelos na FIPE falhou ({}): {}", caminho, ex.getMessage());
				return Optional.empty();
			}
		}).orElseThrow(this::indisponivel);
	}

	@Override
	public List<CatalogOption> years(VehicleKind kind, String brandCode, String modelCode) {
		String marca = exigir(brandCode, "Escolha a marca antes do modelo.");
		String modelo = exigir(modelCode, "Escolha o modelo antes do ano.");
		String caminho = base(kind) + "/marcas/" + marca + "/modelos/" + modelo + "/anos";

		return listar("anos:" + kind + ":" + marca + ":" + modelo, caminho);
	}

	@Override
	public VehicleCatalogDetail detail(VehicleKind kind, String brandCode, String modelCode, String yearCode) {
		String marca = exigir(brandCode, "Escolha a marca.");
		String modelo = exigir(modelCode, "Escolha o modelo.");
		String ano = exigir(yearCode, "Escolha o ano.");

		String caminho = base(kind) + "/marcas/" + marca + "/modelos/" + modelo + "/anos/" + ano;

		return detailCache.get("detalhe:" + kind + ":" + marca + ":" + modelo + ":" + ano, () -> {
			try {
				FipeDetalhe corpo = restClient.get().uri(caminho).retrieve().body(FipeDetalhe.class);
				if (corpo == null || corpo.modelo() == null) {
					return Optional.empty();
				}

				Integer anoModelo = corpo.anoModelo() == null || corpo.anoModelo() == ZERO_KM
						? null
						: corpo.anoModelo();

				return Optional.of(new VehicleCatalogDetail(corpo.codigoFipe(), corpo.marca(), corpo.modelo(),
						anoModelo, traduzirCombustivel(corpo.combustivel()), corpo.combustivel()));
			} catch (Exception ex) {
				log.warn("Consulta de detalhe na FIPE falhou ({}): {}", caminho, ex.getMessage());
				return Optional.empty();
			}
		}).orElseThrow(this::indisponivel);
	}

	// ----------------------------------------------------------- apoio
	/** Os niveis que devolvem uma lista simples de {codigo, nome}. */
	private List<CatalogOption> listar(String chave, String caminho) {
		return optionsCache.get(chave, () -> {
			try {
				FipeItem[] corpo = restClient.get().uri(caminho).retrieve().body(FipeItem[].class);
				if (corpo == null) {
					return Optional.empty();
				}
				return Optional.of(java.util.Arrays.stream(corpo)
						.map(item -> new CatalogOption(String.valueOf(item.codigo()), item.nome()))
						.toList());
			} catch (Exception ex) {
				log.warn("Consulta a FIPE falhou ({}): {}", caminho, ex.getMessage());
				return Optional.empty();
			}
		}).orElseThrow(this::indisponivel);
	}

	private String base(VehicleKind kind) {
		return properties.getFipeUrl() + "/" + kind.getFipePath();
	}

	/**
	 * Falha da API externa vira 400 com uma frase util, nao 500.
	 *
	 * <p>
	 * A tabela FIPE fora do ar nao e um erro do CarPass, e a pessoa nao precisa
	 * saber de quem e a culpa -- precisa saber que da para seguir digitando.
	 * </p>
	 */
	private BusinessException indisponivel() {
		return new BusinessException(
				"A tabela FIPE nao respondeu agora. Tente de novo em instantes ou informe o veiculo manualmente.");
	}

	private String exigir(String valor, String mensagem) {
		if (valor == null || valor.isBlank()) {
			throw new BusinessException(mensagem);
		}
		// O valor entra na URL: so digitos e hifen passam, e nada mais. Sem isto,
		// um parametro com barra poderia apontar a chamada para outro caminho.
		if (!valor.matches("[0-9A-Za-z-]{1,20}")) {
			throw new BusinessException("Codigo invalido na consulta a tabela FIPE.");
		}
		return valor;
	}

	/**
	 * Traduz o combustivel da FIPE para o enum do sistema.
	 *
	 * <p>
	 * Devolve nulo quando o termo nao casa com nenhum dos nossos -- e o
	 * {@code fuelLabel} preserva o texto original. Chutar o mais parecido seria
	 * gravar um dado errado com aparencia de certo.
	 * </p>
	 */
	private FuelType traduzirCombustivel(String combustivel) {
		if (combustivel == null || combustivel.isBlank()) {
			return null;
		}

		String normalizado = combustivel.trim().toLowerCase(Locale.ROOT);

		if (normalizado.contains("flex")) {
			return FuelType.FLEX;
		}
		if (normalizado.contains("álcool") || normalizado.contains("alcool") || normalizado.contains("etanol")) {
			return FuelType.ETHANOL;
		}
		if (normalizado.contains("diesel")) {
			return FuelType.DIESEL;
		}
		if (normalizado.contains("elétr") || normalizado.contains("eletr")) {
			return FuelType.ELECTRIC;
		}
		if (normalizado.contains("híbr") || normalizado.contains("hibr")) {
			return FuelType.HYBRID;
		}
		if (normalizado.contains("gasolina")) {
			return FuelType.GASOLINE;
		}
		return null;
	}

	// ------------------------------------------ corpos da API externa
	/*
	 * Records de fronteira: descrevem o que a FIPE devolve hoje. O ignoreUnknown
	 * e explicito porque um campo novo do lado de la nao pode derrubar o cadastro
	 * de veiculo daqui.
	 */

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record FipeItem(Object codigo, String nome) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record FipeModelos(List<FipeItem> modelos) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record FipeDetalhe(
			@JsonProperty("Valor") String valor,
			@JsonProperty("Marca") String marca,
			@JsonProperty("Modelo") String modelo,
			@JsonProperty("AnoModelo") Integer anoModelo,
			@JsonProperty("Combustivel") String combustivel,
			@JsonProperty("CodigoFipe") String codigoFipe,
			@JsonProperty("MesReferencia") String mesReferencia) {
	}
}
