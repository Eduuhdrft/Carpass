package com.carpass.service_api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Carrega o arquivo {@code .env} da raiz do modulo para dentro do ambiente do
 * Spring, antes de qualquer bean ser criado.
 *
 * <p>
 * Isso faz o projeto funcionar do mesmo jeito rodando pela IDE, pelo
 * {@code ./gradlew bootRun} ou pelo jar, sem que ninguem precise cadastrar
 * variavel de ambiente na mao. O arquivo continua fora do controle de versao,
 * entao nenhum segredo vaza para o repositorio.
 * </p>
 *
 * <p>
 * Precedencia: variaveis de ambiente reais do sistema continuam ganhando do
 * {@code .env}. Assim, em producao basta definir as variaveis normalmente e o
 * arquivo e ignorado.
 * </p>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "carpassDotenv";

	/** Diretorios onde procuramos o .env, do mais provavel ao menos provavel. */
	private static final List<String> CANDIDATE_DIRECTORIES = List.of(".", "..", "../..", "backend/service-api");

	private final Log log;

	public DotenvEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(DotenvEnvironmentPostProcessor.class);
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		for (String directory : CANDIDATE_DIRECTORIES) {
			Path candidate = Paths.get(directory, ".env").normalize();
			if (Files.isRegularFile(candidate)) {
				Map<String, Object> values = parse(candidate);
				if (!values.isEmpty()) {
					// addLast: variaveis reais do sistema tem prioridade sobre o arquivo.
					environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
					log.info("Configuracao carregada de " + candidate.toAbsolutePath() + " (" + values.size()
							+ " variaveis)");
				}
				return;
			}
		}
		log.debug("Nenhum arquivo .env encontrado. Usando apenas as variaveis de ambiente do sistema.");
	}

	private Map<String, Object> parse(Path file) {
		Map<String, Object> values = new LinkedHashMap<>();
		try {
			for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("export ")) {
					line = line.substring("export ".length()).trim();
				}
				int separator = line.indexOf('=');
				if (separator <= 0) {
					continue;
				}
				String key = line.substring(0, separator).trim();
				String value = unquote(line.substring(separator + 1).trim());
				if (!key.isEmpty() && !value.isEmpty()) {
					values.put(key, value);
				}
			}
		} catch (IOException ex) {
			log.warn("Nao foi possivel ler " + file.toAbsolutePath() + ": " + ex.getMessage());
		}
		return values;
	}

	private String unquote(String value) {
		if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
				|| (value.startsWith("'") && value.endsWith("'")))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	@Override
	public int getOrder() {
		// Depois do processamento padrao de configuracao do Boot.
		return Ordered.LOWEST_PRECEDENCE;
	}
}
