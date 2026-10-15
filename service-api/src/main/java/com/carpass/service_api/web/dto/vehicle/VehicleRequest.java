package com.carpass.service_api.web.dto.vehicle;

import java.util.UUID;

import com.carpass.service_api.domain.enums.FuelType;
import com.carpass.service_api.domain.enums.VehicleKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edicao de veiculo.
 *
 * <p>
 * O padrao de placa aceita tanto o formato antigo (ABC1234) quanto o Mercosul
 * (ABC1D23).
 * </p>
 */
public record VehicleRequest(

		@NotNull(message = "Informe o cliente proprietario")
		UUID customerId,

		@NotBlank(message = "Informe a placa")
		@Pattern(regexp = "^[A-Za-z]{3}[0-9][0-9A-Ja-j][0-9]{2}$", message = "Placa invalida. Use o formato ABC1234 ou ABC1D23")
		String plate,

		@NotBlank(message = "Informe a marca")
		@Size(max = 80)
		String brand,

		@NotBlank(message = "Informe o modelo")
		@Size(max = 120)
		String model,

		Integer manufactureYear,

		Integer modelYear,

		@PositiveOrZero(message = "A quilometragem nao pode ser negativa")
		Integer mileage,

		FuelType fuelType,

		/*
		 * Carro, moto ou caminhao. Opcional: o cadastro manual continua valendo, e
		 * exigir o tipo bloquearia o carro que nao esta na FIPE.
		 */
		VehicleKind vehicleKind,

		/* Codigo do veiculo na FIPE, quando escolhido de la. Vazio no manual. */
		@Size(max = 20)
		String fipeCode,

		@Size(max = 30)
		String chassis,

		@Size(max = 20)
		String renavam,

		@Size(max = 40)
		String color,

		@Size(max = 1000)
		String notes) {
}
