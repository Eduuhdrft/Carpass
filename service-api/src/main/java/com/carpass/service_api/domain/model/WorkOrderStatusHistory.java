package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
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
 * Registro de cada transicao de status de uma ordem de servico.
 */
@Entity
@Table(name = "work_order_status_history", indexes = { @Index(name = "idx_wo_status_history_order", columnList = "work_order_id") })
public class WorkOrderStatusHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id", nullable = false)
	private WorkOrder workOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 40)
	private WorkOrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 40)
	private WorkOrderStatus toStatus;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "changed_by_id")
	private User changedBy;

	@Column(name = "note", length = 500)
	private String note;

	public WorkOrderStatusHistory() {
		// construtor sem argumentos exigido pelo JPA
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public WorkOrderStatus getFromStatus() {
		return fromStatus;
	}

	public void setFromStatus(WorkOrderStatus fromStatus) {
		this.fromStatus = fromStatus;
	}

	public WorkOrderStatus getToStatus() {
		return toStatus;
	}

	public void setToStatus(WorkOrderStatus toStatus) {
		this.toStatus = toStatus;
	}

	public User getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(User changedBy) {
		this.changedBy = changedBy;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}
