package com.carpass.service_api.web.dto.catalog;

/**
 * Endereco encontrado a partir do CEP.
 *
 * <p>
 * Nao traz numero nem complemento porque o CEP nao sabe: ele identifica a rua
 * (as vezes o bairro inteiro), nunca a casa. Esses dois campos continuam sendo
 * digitados, e a tela leva o cursor ate o numero depois do preenchimento.
 * </p>
 *
 * @param zipCode      CEP so com digitos
 * @param street       logradouro; vazio em CEP de cidade inteira
 * @param neighborhood bairro; tambem pode vir vazio
 * @param city         municipio
 * @param state        sigla da UF
 */
public record AddressLookupResponse(String zipCode, String street, String neighborhood, String city,
		String state) {
}
