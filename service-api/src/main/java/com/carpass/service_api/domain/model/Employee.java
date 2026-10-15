package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.Role;
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
 * Funcionario da oficina. Pode ou nao ter uma conta de acesso ({@link User}) associada.
 */
@Entity
@Table(name = "employees", indexes = { @Index(name = "idx_employees_workshop", columnList = "workshop_id") })
public class Employee extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 40)
	private Role role;

	@Column(name = "specialty", length = 120)
	private String specialty;

	@Column(name = "phone", length = 30)
	private String phone;

	@Column(name = "email", length = 180)
	private String email;

	@Column(name = "active", nullable = false)
	private boolean active;

	public Employee() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(String specialty) {
		this.specialty = specialty;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
