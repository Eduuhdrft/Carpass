package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cache das consultas externas.
 *
 * <p>
 * O que estes testes protegem: nao repetir pergunta a um servico publico e
 * gratuito, e -- mais importante -- nao guardar erro. Guardar uma falha
 * transformaria uma indisponibilidade de dez segundos em doze horas de CEP que
 * "nao existe".
 * </p>
 */
class ExternalCacheTest {

	@Test
	@DisplayName("o produtor roda uma vez; a segunda leitura vem do cache")
	void producesOnce() {
		ExternalCache<String, String> cache = new ExternalCache<>(Duration.ofMinutes(10), 100);
		AtomicInteger chamadas = new AtomicInteger();

		for (int i = 0; i < 5; i++) {
			cache.get("01310100", () -> {
				chamadas.incrementAndGet();
				return Optional.of("Avenida Paulista");
			});
		}

		assertThat(chamadas.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("resultado vazio nao e guardado")
	void doesNotCacheMisses() {
		ExternalCache<String, String> cache = new ExternalCache<>(Duration.ofMinutes(10), 100);
		AtomicInteger chamadas = new AtomicInteger();

		for (int i = 0; i < 3; i++) {
			cache.get("00000000", () -> {
				chamadas.incrementAndGet();
				return Optional.empty();
			});
		}

		// Tres tentativas, tres idas: uma API fora do ar volta a ser consultada na
		// proxima vez, e nao daqui a doze horas.
		assertThat(chamadas.get()).isEqualTo(3);
		assertThat(cache.size()).isZero();
	}

	@Test
	@DisplayName("entrada vencida e buscada de novo")
	void expiresEntries() throws InterruptedException {
		ExternalCache<String, String> cache = new ExternalCache<>(Duration.ofMillis(40), 100);
		AtomicInteger chamadas = new AtomicInteger();

		cache.get("chave", () -> {
			chamadas.incrementAndGet();
			return Optional.of("primeiro");
		});

		Thread.sleep(80);

		Optional<String> segundo = cache.get("chave", () -> {
			chamadas.incrementAndGet();
			return Optional.of("segundo");
		});

		assertThat(chamadas.get()).isEqualTo(2);
		assertThat(segundo).contains("segundo");
	}

	@Test
	@DisplayName("o teto de entradas e respeitado")
	void respectsMaxEntries() {
		ExternalCache<String, String> cache = new ExternalCache<>(Duration.ofMinutes(10), 3);

		for (int i = 0; i < 10; i++) {
			String chave = "cep-" + i;
			cache.get(chave, () -> Optional.of("valor"));
		}

		// Sem teto, um cache alimentado por entrada de usuario e um vazamento de
		// memoria esperando a hora.
		assertThat(cache.size()).isEqualTo(3);
	}

	@Test
	@DisplayName("o mais recentemente usado sobrevive ao descarte")
	void keepsRecentlyUsed() {
		ExternalCache<String, String> cache = new ExternalCache<>(Duration.ofMinutes(10), 2);

		cache.get("a", () -> Optional.of("A"));
		cache.get("b", () -> Optional.of("B"));
		// Tocar em "a" o torna o mais recente; "b" passa a ser o candidato a sair.
		cache.get("a", () -> Optional.of("ignorado"));
		cache.get("c", () -> Optional.of("C"));

		AtomicInteger recalculou = new AtomicInteger();
		cache.get("a", () -> {
			recalculou.incrementAndGet();
			return Optional.of("recalculado");
		});

		assertThat(recalculou.get()).isZero();
	}
}
