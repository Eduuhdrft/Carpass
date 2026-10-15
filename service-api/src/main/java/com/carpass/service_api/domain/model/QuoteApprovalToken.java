package com.carpass.service_api.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token que permite ao cliente decidir sobre o orcamento sem ter conta.
 *
 * <p>
 * Aqui o token <em>e</em> a autorizacao: quem tem o link decide. Por isso ele
 * recebe o mesmo tratamento do token de senha -- so o hash SHA-256 fica no
 * banco, o uso e unico e a validade e curta. Um vazamento desta tabela nao
 * permite aprovar orcamento nenhum.
 * </p>
 */
@Entity
@Table(name = "quote_approval_tokens",
		indexes = { @Index(name = "idx_quote_approval_order", columnList = "work_order_id") })
public class QuoteApprovalToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id", nullable = false)
	private WorkOrder workOrder;

	@Column(name = "token_hash", nullable = false, unique = true, length = 100)
	private String tokenHash;

	/**
	 * Endereco para onde a proposta foi enviada.
	 *
	 * <p>
	 * Fica na linha, e nao no cadastro do cliente, porque o envio pode usar um
	 * endereco informado so naquele momento. O registro precisa dizer para onde a
	 * proposta realmente foi, nao para onde iria hoje.
	 * </p>
	 */
	@Column(name = "sent_to", nullable = false, length = 180)
	private String sentTo;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	/** APPROVED ou REJECTED; nulo enquanto ninguem decidiu. */
	@Column(name = "decision", length = 20)
	private String decision;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	public QuoteApprovalToken() {
		// construtor sem argumentos exigido pelo JPA
	}

	/** Ainda vale: nao foi usado e nao expirou. */
	public boolean isUsable() {
		return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public String getSentTo() {
		return sentTo;
	}

	public void setSentTo(String sentTo) {
		this.sentTo = sentTo;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public void setUsedAt(LocalDateTime usedAt) {
		this.usedAt = usedAt;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public LocalDateTime getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(LocalDateTime decidedAt) {
		this.decidedAt = decidedAt;
	}
}
