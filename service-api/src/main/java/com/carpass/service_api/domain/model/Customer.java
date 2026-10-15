package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Cliente da oficina (pessoa fisica ou juridica).
 */
@Entity
@Table(name = "customers",
		uniqueConstraints = { @UniqueConstraint(name = "uk_customers_workshop_document", columnNames = { "workshop_id", "document" }) },
		indexes = { @Index(name = "idx_customers_workshop", columnList = "workshop_id"),
				@Index(name = "idx_customers_name", columnList = "name") })
public class Customer extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private CustomerType type;

	@Column(name = "name", nullable = false, length = 180)
	private String name;

	@Column(name = "document", length = 20)
	private String document;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "email", length = 180)
	private String email;

	@Embedded
	private Address address;

	@Column(name = "notes", length = 1000)
	private String notes;

	@Column(name = "active", nullable = false)
	private boolean active;

	public Customer() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public CustomerType getType() {
		return type;
	}

	public void setType(CustomerType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
