package com.carpass.service_api.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache pequeno para respostas de servicos externos.
 *
 * <h2>Por que escrever um em vez de trazer uma biblioteca</h2>
 *
 * <p>
 * O que este projeto precisa cabe em cinquenta linhas: guardar por um tempo,
 * respeitar um teto e descartar o mais antigo. Um Caffeine da vida resolveria o
 * mesmo com mais dependencia, mais superficie e uma configuracao a mais para
 * alguem entender daqui a um ano.
 * </p>
 *
 * <h2>O que ele nao faz, de proposito</h2>
 *
 * <p>
 * Nao guarda erro. Se a API externa falhou, a proxima tentativa vai de novo la
 * -- guardar a falha transformaria uma indisponibilidade de dez segundos em doze
 * horas de CEP que "nao existe".
 * </p>
 *
 * <p>
 * <strong>O cache e por instancia.</strong> Com mais de um backend rodando, cada
 * um mantem o proprio. Para o volume de uma oficina isso e irrelevante; se um
 * dia deixar de ser, o lugar de mudar e aqui.
 * </p>
 */
class ExternalCache<K, V> {

	private record Entry<V>(V value, Instant expiresAt) {
	}

	private final Duration ttl;
	private final Map<K, Entry<V>> entries;

	ExternalCache(Duration ttl, int maxEntries) {
		this.ttl = ttl;

		// LinkedHashMap em modo de acesso + removeEldest = LRU pronto, sincronizado
		// por fora. O acesso e raro e curto, entao a trava simples basta.
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, Entry<V>> eldest) {
				return size() > maxEntries;
			}
		};
	}

	/**
	 * Devolve o valor guardado ou chama o produtor.
	 *
	 * <p>
	 * O produtor roda fora da trava: ele faz chamada de rede, e segurar a trava
	 * durante uma ida a internet faria uma consulta lenta parar todas as outras.
	 * O preco e que duas requisicoes simultaneas para o mesmo CEP podem buscar as
	 * duas -- desperdicio pequeno e aceitavel, ao contrario do contrario.
	 * </p>
	 *
	 * @param producer devolve vazio quando nao ha o que guardar; nada e gravado
	 */
	Optional<V> get(K key, Supplier<Optional<V>> producer) {
		Instant agora = Instant.now();

		synchronized (entries) {
			Entry<V> guardado = entries.get(key);
			if (guardado != null && guardado.expiresAt().isAfter(agora)) {
				return Optional.of(guardado.value());
			}
			// Vencido: sai agora para nao ocupar espaco ate a proxima leitura.
			if (guardado != null) {
				entries.remove(key);
			}
		}

		Optional<V> produzido = producer.get();
		produzido.ifPresent(valor -> {
			synchronized (entries) {
				entries.put(key, new Entry<>(valor, Instant.now().plus(ttl)));
			}
		});

		return produzido;
	}

	/** Só para teste: quantas entradas vivas existem agora. */
	int size() {
		synchronized (entries) {
			return entries.size();
		}
	}
}
