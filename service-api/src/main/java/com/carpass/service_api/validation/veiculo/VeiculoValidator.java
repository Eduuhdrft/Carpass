package com.carpass.service_api.validation.veiculo;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.carpass.service_api.exceptions.RegraDeNegocioException;

@Component
public class VeiculoValidator {
	
	public void validarAnoDeFabricacao(Integer anoDeFabricacao) {
		Integer anoAtual = LocalDate.now().getYear();
		
		if(anoDeFabricacao > anoAtual) {
			throw new RegraDeNegocioException("Ano de fabricação inexistente");
		}
	}
	
}
