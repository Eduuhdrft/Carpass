package com.carpass.service_api.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Base de todas as entidades: identificador UUID e carimbos de auditoria.
 *
 * <p>
 * O {@code equals}/{@code hashCode} usa apenas o id e trata entidades ainda nao
 * persistidas como diferentes entre si, que e o comportamento seguro com JPA.
 * </p>
 */
@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	/** True enquanto a entidade ainda nao foi persistida. */
	public boolean isNew() {
		return id == null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BaseEntity entity)) {
			return false;
		}
		return id != null && id.equals(entity.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass().getSimpleName());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id + "]";
	}
}
