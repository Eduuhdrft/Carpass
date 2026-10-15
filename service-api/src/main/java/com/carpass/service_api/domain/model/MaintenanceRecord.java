package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item do historico do veiculo. Gerado ao entregar uma OS ou lancado manualmente para servicos feitos fora da oficina.
 */
@Entity
@Table(name = "maintenance_records", indexes = { @Index(name = "idx_maintenance_vehicle", columnList = "vehicle_id") })
public class MaintenanceRecord extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "work_order_id")
	private WorkOrder workOrder;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	@Column(name = "mileage")
	private Integer mileage;

	@Column(name = "description", nullable = false, length = 500)
	private String description;

	@Column(name = "performed_by", length = 150)
	private String performedBy;

	@Column(name = "amount", precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "notes", length = 1000)
	private String notes;

	public MaintenanceRecord() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public LocalDate getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(LocalDate serviceDate) {
		this.serviceDate = serviceDate;
	}

	public Integer getMileage() {
		return mileage;
	}

	public void setMileage(Integer mileage) {
		this.mileage = mileage;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPerformedBy() {
		return performedBy;
	}

	public void setPerformedBy(String performedBy) {
		this.performedBy = performedBy;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
