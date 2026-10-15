package com.carpass.service_api.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma saida de dinheiro da oficina.
 *
 * <h2>Por que a data e {@code LocalDate}, e nao {@code LocalDateTime}</h2>
 *
 * <p>
 * Despesa pertence a um dia, nao a um instante: a conta de luz e "de 12 de
 * marco", nao "de 12 de marco as 14h32". Guardar hora criaria uma precisao que
 * ninguem preenche e abriria a porta para o erro classico de fuso -- uma despesa
 * do dia 1o as 00h caindo no mes anterior no relatorio.
 * </p>
 *
 * <h2>O responsavel fica registrado</h2>
 *
 * <p>
 * {@code createdBy} responde "quem lancou isso?" sem depender da memoria de
 * ninguem. E {@code LAZY} e opcional: se a conta for desativada um dia, a
 * despesa continua valendo.
 * </p>
 */
@Entity
@Table(name = "expenses",
		indexes = { @Index(name = "idx_expenses_workshop_date", columnList = "workshop_id, expense_date"),
				@Index(name = "idx_expenses_category", columnList = "category_id") })
public class Expense extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "description", nullable = false, length = 180)
	private String description;

	/**
	 * Valor da despesa, sempre positivo.
	 *
	 * <p>
	 * A restricao vive tambem no banco ({@code ck_expenses_amount_positive}): a
	 * validacao do DTO cobre a API, e o CHECK cobre qualquer caminho que um dia
	 * escreva direto na tabela. Valor negativo aqui viraria receita disfarcada de
	 * despesa no calculo do liquido.
	 * </p>
	 */
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private ExpenseCategory category;

	@Column(name = "notes", length = 1000)
	private String notes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	public Expense() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public void setExpenseDate(LocalDate expenseDate) {
		this.expenseDate = expenseDate;
	}

	public ExpenseCategory getCategory() {
		return category;
	}

	public void setCategory(ExpenseCategory category) {
		this.category = category;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}
}
