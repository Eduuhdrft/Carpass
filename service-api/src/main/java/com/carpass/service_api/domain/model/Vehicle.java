package com.carpass.service_api.domain.model;

import com.carpass.service_api.domain.enums.FuelType;
import com.carpass.service_api.domain.enums.VehicleKind;
import jakarta.persistence.Column;
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
 * Veiculo atendido pela oficina, sempre vinculado a um cliente.
 */
@Entity
@Table(name = "vehicles",
		uniqueConstraints = { @UniqueConstraint(name = "uk_vehicles_workshop_plate", columnNames = { "workshop_id", "plate" }) },
		indexes = { @Index(name = "idx_vehicles_workshop", columnList = "workshop_id"),
				@Index(name = "idx_vehicles_customer", columnList = "customer_id") })
public class Vehicle extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workshop_id", nullable = false)
	private Workshop workshop;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "plate", nullable = false, length = 10)
	private String plate;

	@Column(name = "brand", nullable = false, length = 80)
	private String brand;

	@Column(name = "model", nullable = false, length = 120)
	private String model;

	@Column(name = "manufacture_year")
	private Integer manufactureYear;

	@Column(name = "model_year")
	private Integer modelYear;

	@Column(name = "mileage")
	private Integer mileage;

	@Enumerated(EnumType.STRING)
	@Column(name = "fuel_type", length = 20)
	private FuelType fuelType;

	/**
	 * Carro, moto ou caminhao.
	 *
	 * <p>
	 * Opcional porque os veiculos cadastrados antes desta coluna existir nao tem
	 * como saber. Serve para reabrir o seletor no lugar certo ao editar -- a FIPE
	 * e tres tabelas separadas, e sem o tipo nao da para voltar a lista de marcas
	 * de onde aquele veiculo veio.
	 * </p>
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "vehicle_kind", length = 20)
	private VehicleKind vehicleKind;

	/**
	 * Codigo do veiculo na tabela FIPE, quando veio de la.
	 *
	 * <p>
	 * Vazio no cadastro manual, e isso e normal: carro antigo, importado raro ou
	 * adaptado simplesmente nao esta na FIPE. Guardado, identifica aquele modelo e
	 * versao de forma estavel, independente de como o nome foi escrito -- e abre a
	 * porta para consultar valor de mercado sem pedir nada de novo.
	 * </p>
	 */
	@Column(name = "fipe_code", length = 20)
	private String fipeCode;

	@Column(name = "chassis", length = 30)
	private String chassis;

	@Column(name = "renavam", length = 20)
	private String renavam;

	@Column(name = "color", length = 40)
	private String color;

	@Column(name = "notes", length = 1000)
	private String notes;

	@Column(name = "active", nullable = false)
	private boolean active;

	public Vehicle() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Workshop getWorkshop() {
		return workshop;
	}

	public void setWorkshop(Workshop workshop) {
		this.workshop = workshop;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getPlate() {
		return plate;
	}

	public void setPlate(String plate) {
		this.plate = plate;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getManufactureYear() {
		return manufactureYear;
	}

	public void setManufactureYear(Integer manufactureYear) {
		this.manufactureYear = manufactureYear;
	}

	public Integer getModelYear() {
		return modelYear;
	}

	public void setModelYear(Integer modelYear) {
		this.modelYear = modelYear;
	}

	public Integer getMileage() {
		return mileage;
	}

	public void setMileage(Integer mileage) {
		this.mileage = mileage;
	}

	public FuelType getFuelType() {
		return fuelType;
	}

	public void setFuelType(FuelType fuelType) {
		this.fuelType = fuelType;
	}

	public VehicleKind getVehicleKind() {
		return vehicleKind;
	}

	public void setVehicleKind(VehicleKind vehicleKind) {
		this.vehicleKind = vehicleKind;
	}

	public String getFipeCode() {
		return fipeCode;
	}

	public void setFipeCode(String fipeCode) {
		this.fipeCode = fipeCode;
	}

	public String getChassis() {
		return chassis;
	}

	public void setChassis(String chassis) {
		this.chassis = chassis;
	}

	public String getRenavam() {
		return renavam;
	}

	public void setRenavam(String renavam) {
		this.renavam = renavam;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
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
