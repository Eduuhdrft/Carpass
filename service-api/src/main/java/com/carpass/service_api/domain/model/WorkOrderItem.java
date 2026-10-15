package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Servico executado ou peca aplicada dentro de uma ordem de servico.
 */
@Entity
@Table(name = "work_order_items", indexes = { @Index(name = "idx_work_order_items_order", columnList = "work_order_id") })
public class WorkOrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "work_order_id")
	private WorkOrder workOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ItemType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "part_id")
	private Part part;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "service_catalog_id")
	private ServiceCatalog serviceCatalog;

	@Column(name = "description", nullable = false, length = 300)
	private String description;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "total_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalPrice;

	public WorkOrderItem() {
		// construtor sem argumentos exigido pelo JPA
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public ItemType getType() {
		return type;
	}

	public void setType(ItemType type) {
		this.type = type;
	}

	public Part getPart() {
		return part;
	}

	public void setPart(Part part) {
		this.part = part;
	}

	public ServiceCatalog getServiceCatalog() {
		return serviceCatalog;
	}

	public void setServiceCatalog(ServiceCatalog serviceCatalog) {
		this.serviceCatalog = serviceCatalog;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

}
