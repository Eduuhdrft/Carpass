package com.carpass.service_api.models;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "manutencao")
public class Manutencao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;
	
	@ManyToOne
	@JoinColumn(name = "veiculo_id", nullable = false)
	private Veiculo veiculo;
	
	@Column(nullable = false)
	private String placa;
	
	@Column(name = "tipo_servico", nullable = false)
	private String tipoServico;
	
	@Column(nullable = false)
	private LocalDate data_manutencao;
	
	@Column(nullable = false)
	private Integer km_momento;
	
	@Column(precision = 10, scale = 2)
	private BigDecimal valor;
	
	@Column(name = "descricao", nullable = false)
	private String descricaoManutencao;
	
	@Column(name = "nome_oficina", nullable = false)
	private String nomeOficina;
	
	public Manutencao() {}

	public Manutencao(UUID id, Veiculo veiculo, String placa, String tipoServico, LocalDate data_manutencao,
			Integer km_momento, BigDecimal valor, String descricaoManutencao, String nomeOficina) {
		this.id = id;
		this.veiculo = veiculo;
		this.placa = placa;
		this.tipoServico = tipoServico;
		this.data_manutencao = data_manutencao;
		this.km_momento = km_momento;
		this.valor = valor;
		this.descricaoManutencao = descricaoManutencao;
		this.nomeOficina = nomeOficina;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public String getPlaca() {
		return placa;
	}

	public void setPlaca(String placa) {
		this.placa = placa;
	}

	public String getTipoServico() {
		return tipoServico;
	}

	public void setTipoServico(String tipoServico) {
		this.tipoServico = tipoServico;
	}

	public LocalDate getData_manutencao() {
		return data_manutencao;
	}

	public void setData_manutencao(LocalDate data_manutencao) {
		this.data_manutencao = data_manutencao;
	}

	public Integer getKm_momento() {
		return km_momento;
	}

	public void setKm_momento(Integer km_momento) {
		this.km_momento = km_momento;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public String getDescricaoManutencao() {
		return descricaoManutencao;
	}

	public void setDescricaoManutencao(String descricaoManutencao) {
		this.descricaoManutencao = descricaoManutencao;
	}

	public String getNomeOficina() {
		return nomeOficina;
	}

	public void setNomeOficina(String nomeOficina) {
		this.nomeOficina = nomeOficina;
	}

	@Override
	public String toString() {
		return "Manutencoes [id=" + id + ", veiculo=" + veiculo + ", placa=" + placa + ", tipoServico="
				+ tipoServico + ", data_manutencao=" + data_manutencao + ", km_momento=" + km_momento + ", valor=" + valor
				+ ", descricaoManutencao=" + descricaoManutencao + ", nomeOficina=" + nomeOficina + "]";
	}
	
}
