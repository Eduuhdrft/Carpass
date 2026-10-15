package com.carpass.service_api.domain;

import java.util.List;

/**
 * Lista com que toda oficina comeca.
 *
 * <h2>Por que existe</h2>
 *
 * <p>
 * Cadastro vazio e cadastro que ninguem preenche: quem acabou de entrar no
 * sistema quer lancar a primeira peca, nao parar para inventar uma taxonomia.
 * Comecar com o vocabulario que qualquer oficina reconhece tira esse obstaculo
 * -- e como e cadastro, quem discordar renomeia, desativa ou cria o proprio.
 * </p>
 *
 * <h2>Cuidado ao mexer</h2>
 *
 * <p>
 * Esta lista vale para oficina <strong>nova</strong>. As que ja existem foram
 * semeadas pela migration {@code V4}, que carrega uma copia destes nomes em SQL.
 * As duas nao se sincronizam de proposito: migration e registro historico e nao
 * pode mudar depois de aplicada. Incluir um tipo aqui atende quem chegar depois;
 * para as oficinas antigas, seria preciso uma migration nova.
 * </p>
 */
public final class DefaultPartTypes {

	/**
	 * Nome e a explicacao que aparece na tela.
	 *
	 * @param name        como o tipo aparece nas listas e filtros
	 * @param description ajuda a distinguir tipos vizinhos -- "oleo de motor" e
	 *                    "oleo de cambio" so parecem obvios para quem ja sabe
	 */
	public record Seed(String name, String description) {
	}

	private static final List<Seed> SEEDS = List.of(
			new Seed("Oleo de motor", "Lubrificante do motor, mineral, semissintetico ou sintetico."),
			new Seed("Oleo de cambio", "Lubrificante da caixa de cambio, manual ou automatica."),
			new Seed("Oleo hidraulico", "Fluido do sistema hidraulico, inclusive direcao."),
			new Seed("Fluido de freio", "Fluido do sistema de freio (DOT3, DOT4, DOT5.1)."),
			new Seed("Aditivo", "Aditivo de radiador, de combustivel e afins."),
			new Seed("Filtro", "Filtro de oleo, de ar, de combustivel e de cabine."),
			new Seed("Vela", "Vela de ignicao."),
			new Seed("Bobina", "Bobina de ignicao."),
			new Seed("Bateria", "Bateria automotiva."),
			new Seed("Correia", "Correia dentada, do alternador e tensores."),
			new Seed("Pastilha e disco de freio", "Componentes de atrito do sistema de freio."),
			new Seed("Suspensao", "Amortecedor, mola, batente, bandeja e buchas."),
			new Seed("Embreagem", "Kit de embreagem, disco, plato e atuador."),
			new Seed("Sensor", "Sensores de motor, freio e cambio."),
			new Seed("Lampada", "Lampadas de farol, lanterna e painel."),
			new Seed("Pneu", "Pneus e camaras."),
			new Seed("Eletrica", "Fusiveis, reles, chicotes e conectores."),
			new Seed("Arrefecimento", "Radiador, bomba d'agua, valvula termostatica e mangueiras."),
			new Seed("Escapamento", "Silencioso, catalisador e coletores."),
			new Seed("Junta e retentor", "Juntas, retentores e vedacoes."),
			new Seed("Diversos", "O que nao se encaixa nos demais tipos."));

	public static List<Seed> all() {
		return SEEDS;
	}

	private DefaultPartTypes() {
		// classe utilitaria
	}
}
