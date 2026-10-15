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
 * Onde o dinheiro sai: aluguel, energia, impostos, fornecedores...
 *
 * <p>
 * Cadastro da oficina, pelo mesmo motivo do tipo de peca: a lista nunca esta
 * pronta, e como enum incluir uma categoria exigiria alterar o codigo e publicar
 * de novo. Toda oficina nasce com a lista padrao e edita a partir dela.
 * </p>
 *
 * <p>
 * <strong>Renomear muda o passado.</strong> A despesa aponta para a categoria,
 * entao renomear "Energia eletrica" para "Luz" reescreve tambem os relatorios de
 * meses fechados. E o comportamento esperado de um cadastro -- mas vale saber
 * antes de renomear no meio do exercicio.
 * </p>
 */
@Entity
@Table(name = "expense_categories",
		uniqueConstraints = { @UniqueConstraint(name = "uk_expense_categories_workshop_name",
				columnNames = { "workshop_id", "name" }) },
		indexes = { @Index(name = "idx_expense_categories_workshop", columnList = "workshop_id") })
public class ExpenseCategory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "description", length = 300)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	public ExpenseCategory() {
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
