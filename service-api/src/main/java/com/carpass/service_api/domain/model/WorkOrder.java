package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.QuoteStatus;
import com.carpass.service_api.domain.enums.WorkOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordem de servico: o documento central da operacao da oficina.
 */
@Entity
@Table(name = "work_orders",
		uniqueConstraints = { @UniqueConstraint(name = "uk_work_orders_workshop_number", columnNames = { "workshop_id", "number" }) },
		indexes = { @Index(name = "idx_work_orders_workshop", columnList = "workshop_id"),
				@Index(name = "idx_work_orders_status", columnList = "status"),
				@Index(name = "idx_work_orders_vehicle", columnList = "vehicle_id") })
public class WorkOrder extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "number", nullable = false)
	private Integer number;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "assigned_mechanic_id")
	private Employee assignedMechanic;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private WorkOrderStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "quote_status", nullable = false, length = 20)
	private QuoteStatus quoteStatus;

	@Column(name = "reported_problem", length = 2000)
	private String reportedProblem;

	@Column(name = "diagnosis", length = 2000)
	private String diagnosis;

	@Column(name = "notes", length = 2000)
	private String notes;

	/**
	 * Por que o cliente recusou o orcamento.
	 *
	 * <p>
	 * Fica aqui, e nao apenas na linha do tempo, porque e o dado que decide o
	 * proximo passo da oficina. Guardado no historico ele so seria encontrado
	 * rolando a ordem inteira, e deixaria de ser o "ultimo evento" assim que
	 * alguem movesse a etapa depois da recusa.
	 * </p>
	 */
	@Column(name = "quote_rejection_reason", length = 500)
	private String quoteRejectionReason;

	@Column(name = "mileage")
	private Integer mileage;

	@Column(name = "discount", precision = 12, scale = 2)
	private BigDecimal discount;

	@Column(name = "parts_total", precision = 12, scale = 2)
	private BigDecimal partsTotal;

	@Column(name = "services_total", precision = 12, scale = 2)
	private BigDecimal servicesTotal;

	@Column(name = "total_amount", precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "estimated_delivery_date")
	private LocalDate estimatedDeliveryDate;

	@Column(name = "opened_at", nullable = false)
	private LocalDateTime openedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<WorkOrderItem> items = new ArrayList<>();

	public WorkOrder() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public Employee getAssignedMechanic() {
		return assignedMechanic;
	}

	public void setAssignedMechanic(Employee assignedMechanic) {
		this.assignedMechanic = assignedMechanic;
	}

	public WorkOrderStatus getStatus() {
		return status;
	}

	public void setStatus(WorkOrderStatus status) {
		this.status = status;
	}

	public QuoteStatus getQuoteStatus() {
		return quoteStatus;
	}

	public void setQuoteStatus(QuoteStatus quoteStatus) {
		this.quoteStatus = quoteStatus;
	}

	public String getReportedProblem() {
		return reportedProblem;
	}

	public void setReportedProblem(String reportedProblem) {
		this.reportedProblem = reportedProblem;
	}

	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
	}

	public String getNotes() {
		return notes;
	}

	public String getQuoteRejectionReason() {
		return quoteRejectionReason;
	}

	public void setQuoteRejectionReason(String quoteRejectionReason) {
		this.quoteRejectionReason = quoteRejectionReason;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Integer getMileage() {
		return mileage;
	}

	public void setMileage(Integer mileage) {
		this.mileage = mileage;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	public BigDecimal getPartsTotal() {
		return partsTotal;
	}

	public void setPartsTotal(BigDecimal partsTotal) {
		this.partsTotal = partsTotal;
	}

	public BigDecimal getServicesTotal() {
		return servicesTotal;
	}

	public void setServicesTotal(BigDecimal servicesTotal) {
		this.servicesTotal = servicesTotal;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public LocalDate getEstimatedDeliveryDate() {
		return estimatedDeliveryDate;
	}

	public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
		this.estimatedDeliveryDate = estimatedDeliveryDate;
	}

	public LocalDateTime getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(LocalDateTime openedAt) {
		this.openedAt = openedAt;
	}

	public LocalDateTime getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(LocalDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(LocalDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

	public LocalDateTime getDeliveredAt() {
		return deliveredAt;
	}

	public void setDeliveredAt(LocalDateTime deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	public LocalDateTime getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(LocalDateTime canceledAt) {
		this.canceledAt = canceledAt;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public List<WorkOrderItem> getItems() {
		return items;
	}

	public void setItems(List<WorkOrderItem> items) {
		this.items = items;
	}

	/** Adiciona um item mantendo os dois lados da relacao consistentes. */
	public void addItem(WorkOrderItem item) {
		item.setWorkOrder(this);
		this.items.add(item);
	}

	/** Remove um item mantendo os dois lados da relacao consistentes. */
	public void removeItem(WorkOrderItem item) {
		item.setWorkOrder(null);
		this.items.remove(item);
	}

	/** Limpa todos os itens acionando o orphanRemoval. */
	public void clearItems() {
		for (WorkOrderItem item : new ArrayList<>(this.items)) {
			removeItem(item);
		}
	}

}
