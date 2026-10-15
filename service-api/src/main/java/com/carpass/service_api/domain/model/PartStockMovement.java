package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Movimentacao de estoque de uma peca. Garante rastreabilidade de toda entrada e saida.
 */
@Entity
@Table(name = "part_stock_movements", indexes = { @Index(name = "idx_stock_movements_part", columnList = "part_id") })
public class PartStockMovement extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "part_id", nullable = false)
	private Part part;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "work_order_id")
	private WorkOrder workOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private StockMovementType type;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "previous_stock")
	private Integer previousStock;

	@Column(name = "resulting_stock")
	private Integer resultingStock;

	@Column(name = "reason", length = 300)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	public PartStockMovement() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public Part getPart() {
		return part;
	}

	public void setPart(Part part) {
		this.part = part;
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public StockMovementType getType() {
		return type;
	}

	public void setType(StockMovementType type) {
		this.type = type;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getPreviousStock() {
		return previousStock;
	}

	public void setPreviousStock(Integer previousStock) {
		this.previousStock = previousStock;
	}

	public Integer getResultingStock() {
		return resultingStock;
	}

	public void setResultingStock(Integer resultingStock) {
		this.resultingStock = resultingStock;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

}
