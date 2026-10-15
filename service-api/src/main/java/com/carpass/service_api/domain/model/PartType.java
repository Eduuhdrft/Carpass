package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * O que a peca e: oleo de motor, oleo de cambio, vela, bobina, aditivo...
 *
 * <h2>Por que isto e uma tabela, e nao um texto na peca</h2>
 *
 * <p>
 * Antes existia uma "categoria" digitada a mao. Digitada a mao, "Filtros",
 * "filtro" e "FILTRO" viram tres coisas diferentes para o banco e uma so para
 * quem trabalha -- e nesse ponto nenhum filtro de tela funciona direito. Sendo
 * cadastro, cada tipo tem identidade propria e a pergunta que o estoque precisa
 * responder passa a ter resposta: "quais marcas de oleo de motor eu tenho?".
 * </p>
 *
 * <h2>Por que e cadastro da oficina, e nao lista fixa no codigo</h2>
 *
 * <p>
 * A lista de tipos nunca esta pronta: cada oficina trabalha com um conjunto
 * diferente e amanha aparece um que ninguem previu. Como enum, incluir um tipo
 * novo exigiria alterar o codigo e publicar de novo. Como cadastro, a oficina
 * resolve sozinha -- e toda oficina nasce com uma lista padrao para nao comecar
 * do zero.
 * </p>
 */
/*
 * A restricao declarada aqui compara o nome com a caixa, e o indice criado pela
 * migration compara em minusculas -- porque e a caixa que reabriria o problema
 * que o tipo veio resolver. A diferenca so aparece no schema gerado para os
 * testes; em qualquer caminho quem barra o nome repetido antes do banco e o
 * `existsByWorkshopIdAndNameIgnoreCase` no servico, com mensagem para a pessoa.
 */
@Entity
@Table(name = "part_types",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_part_types_workshop_name", columnNames = { "workshop_id", "name" }) },
		indexes = { @Index(name = "idx_part_types_workshop", columnList = "workshop_id") })
public class PartType extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "description", length = 300)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public PartType() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
