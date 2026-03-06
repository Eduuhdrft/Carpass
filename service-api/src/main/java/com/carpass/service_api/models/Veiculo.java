package com.carpass.service_api.models;

import java.util.UUID;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculo")
public class Veiculo {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;
	
	@Column(unique = true, nullable = false)
	private String placa;
	
	@Column(nullable = false)
	private String marca;
	
	@Column(nullable = false)
	private String modelo;
	
	@Column(name = "ano_fabricacao")
	private Integer anoDeFabricacao;
	
	private String cor;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;
	
	public Veiculo() {}

	public Veiculo(UUID id, String placa, String marca, String modelo, Integer anoDeFabricacao, String cor, Usuario usuario) {
		this.id = id;
		this.placa = placa;
		this.marca = marca;
		this.modelo = modelo;
		this.anoDeFabricacao = anoDeFabricacao;
		this.cor = cor;
		this.usuario = usuario;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getPlaca() {
		return placa;
	}

	public void setPlaca(String placa) {
		this.placa = placa;
	}

	public String getMarca() {
		return marca;
	}

	public void setMarca(String marca) {
		this.marca = marca;
	}

	public String getModelo() {
		return modelo;
	}

	public void setModelo(String modelo) {
		this.modelo = modelo;
	}

	public Integer getAnoDeFabricacao() {
		return anoDeFabricacao;
	}

	public void setAnoDeFabricacao(Integer anoDeFabricacao) {
		this.anoDeFabricacao = anoDeFabricacao;
	}

	public String getCor() {
		return cor;
	}

	public void setCor(String cor) {
		this.cor = cor;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	@Override
	public String toString() {
		return "Veiculo [id=" + id + ", placa=" + placa + ", marca=" + marca + ", modelo=" + modelo
				+ ", anoDeFabricacao=" + anoDeFabricacao + ", cor=" + cor + ", usuario="
				+ usuario + "]";
	}
	
}
