package com.carpass.service_api.models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "codigo_validacao")
public class CodigoValidacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;
	
	private Integer codigo;
	
	private LocalDateTime dataExpiracao;
	
	boolean usado;

	public CodigoValidacao() {}

	public CodigoValidacao(UUID id, Usuario usuario, Integer codigo, LocalDateTime dataExpiracao, boolean usado) {
		super();
		this.id = id;
		this.usuario = usuario;
		this.codigo = codigo;
		this.dataExpiracao = dataExpiracao;
		this.usado = usado;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Integer getCodigo() {
		return codigo;
	}

	public void setCodigo(Integer codigo) {
		this.codigo = codigo;
	}

	public LocalDateTime getDataExpiracao() {
		return dataExpiracao;
	}

	public void setDataExpiracao(LocalDateTime dataExpiracao) {
		this.dataExpiracao = dataExpiracao;
	}

	public boolean isUsado() {
		return usado;
	}

	public void setUsado(boolean usado) {
		this.usado = usado;
	}

	@Override
	public String toString() {
		return "CodigoValidacao [id=" + id + ", usuario=" + usuario + ", codigo=" + codigo + ", dataExpiracao="
				+ dataExpiracao + ", usado=" + usado + "]";
	}
	
}
