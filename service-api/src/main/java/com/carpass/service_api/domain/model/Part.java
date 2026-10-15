package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Peca do estoque da oficina.
 */
@Entity
@Table(name = "parts",
		uniqueConstraints = { @UniqueConstraint(name = "uk_parts_workshop_code", columnNames = { "workshop_id", "code" }) },
		indexes = { @Index(name = "idx_parts_workshop", columnList = "workshop_id"),
				@Index(name = "idx_parts_name", columnList = "name"),
				@Index(name = "idx_parts_type", columnList = "part_type_id"),
				@Index(name = "idx_parts_manufacturer", columnList = "workshop_id, manufacturer") })
public class Part extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Column(name = "code", length = 60)
	private String code;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "manufacturer", length = 120)
	private String manufacturer;

	/**
	 * O que a peca e, escolhido do cadastro de tipos.
	 *
	 * <p>
	 * Opcional de proposito: peca importada de planilha ou lancada as pressas no
	 * balcao entra sem tipo e e classificada depois. Obrigar aqui faria alguem
	 * escolher qualquer um so para salvar, e um tipo errado e pior que nenhum.
	 * </p>
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "part_type_id")
	private PartType partType;

	@Column(name = "cost_price", precision = 12, scale = 2)
	private BigDecimal costPrice;

	@Column(name = "sale_price", precision = 12, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@Column(name = "minimum_stock", nullable = false)
	private Integer minimumStock;

	@Column(name = "active", nullable = false)
	private boolean active;

	public Part() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public PartType getPartType() {
		return partType;
	}

	public void setPartType(PartType partType) {
		this.partType = partType;
	}

	public BigDecimal getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(BigDecimal costPrice) {
		this.costPrice = costPrice;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public Integer getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(Integer stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public Integer getMinimumStock() {
		return minimumStock;
	}

	public void setMinimumStock(Integer minimumStock) {
		this.minimumStock = minimumStock;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/** Indica se o estoque atual esta igual ou abaixo do minimo configurado. */
	public boolean isBelowMinimumStock() {
		return stockQuantity != null && minimumStock != null && stockQuantity <= minimumStock;
	}

}
