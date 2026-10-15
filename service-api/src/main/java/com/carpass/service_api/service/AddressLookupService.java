package com.carpass.service_api.service;

import com.carpass.service_api.web.dto.catalog.AddressLookupResponse;

/**
 * Busca de endereco por CEP.
 */
public interface AddressLookupService {

	/**
	 * Endereco do CEP informado.
	 *
	 * @param zipCode CEP com ou sem mascara
	 * @throws com.carpass.service_api.exception.BusinessException          quando o
	 *                                                                     CEP nao
	 *                                                                     tem oito
	 *                                                                     digitos
	 * @throws com.carpass.service_api.exception.ResourceNotFoundException  quando
	 *                                                                     nenhuma
	 *                                                                     fonte
	 *                                                                     conhece o
	 *                                                                     CEP
	 */
	AddressLookupResponse findByZipCode(String zipCode);
}
