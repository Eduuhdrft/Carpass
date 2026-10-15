package com.carpass.service_api.domain;

import java.util.List;

/**
 * Lista de categorias de despesa com que toda oficina comeca.
 *
 * <p>
 * Mesma ideia de {@link DefaultPartTypes}: cadastro vazio e cadastro que ninguem
 * preenche, e quem acabou de entrar quer lancar a conta de luz, nao inventar um
 * plano de contas. Quem discordar renomeia, desativa ou cria o proprio.
 * </p>
 *
 * <p>
 * <strong>Cuidado ao mexer:</strong> esta lista vale para oficina nova. As que
 * ja existem foram semeadas pela migration {@code V5}, que carrega uma copia
 * destes nomes em SQL. As duas nao se sincronizam de proposito -- migration e
 * registro historico e nao pode mudar depois de aplicada.
 * </p>
 */
public final class DefaultExpenseCategories {

	/**
	 * Nome e a explicacao que aparece na tela.
	 *
	 * @param name        como a categoria aparece nas listas, filtros e graficos
	 * @param description ajuda a decidir onde lancar quando duas parecem servir
	 */
	public record Seed(String name, String description) {
	}

	private static final List<Seed> SEEDS = List.of(
			new Seed("Aluguel", "Aluguel do imovel da oficina."),
			new Seed("Energia eletrica", "Conta de luz."),
			new Seed("Agua", "Conta de agua e esgoto."),
			new Seed("Internet", "Internet, telefone e servicos de comunicacao."),
			new Seed("Funcionarios", "Salarios, encargos e beneficios da equipe."),
			new Seed("Pecas", "Compra de pecas para revenda ou reposicao de estoque."),
			new Seed("Ferramentas", "Ferramentas manuais e de consumo."),
			new Seed("Equipamentos", "Elevador, compressor, scanner e afins."),
			new Seed("Impostos", "Tributos e taxas."),
			new Seed("Fornecedores", "Pagamentos a fornecedores e servicos terceirizados."),
			new Seed("Manutencao", "Manutencao do imovel, dos equipamentos e da frota."),
			new Seed("Marketing", "Divulgacao, anuncios e material grafico."),
			new Seed("Outros", "O que nao se encaixa nas demais categorias."));

	public static List<Seed> all() {
		return SEEDS;
	}

	private DefaultExpenseCategories() {
		// classe utilitaria
	}
}
