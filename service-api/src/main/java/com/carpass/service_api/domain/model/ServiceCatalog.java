package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Servico oferecido pela oficina (troca de oleo, alinhamento, revisao...).
 */
@Entity
@Table(name = "service_catalog", indexes = { @Index(name = "idx_service_catalog_workshop", columnList = "workshop_id") })
public class ServiceCatalog extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "default_price", precision = 12, scale = 2)
	private BigDecimal defaultPrice;

	@Column(name = "estimated_minutes")
	private Integer estimatedMinutes;

	@Column(name = "active", nullable = false)
	private boolean active;

	public ServiceCatalog() {
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

	public BigDecimal getDefaultPrice() {
		return defaultPrice;
	}

	public void setDefaultPrice(BigDecimal defaultPrice) {
		this.defaultPrice = defaultPrice;
	}

	public Integer getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public void setEstimatedMinutes(Integer estimatedMinutes) {
		this.estimatedMinutes = estimatedMinutes;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
