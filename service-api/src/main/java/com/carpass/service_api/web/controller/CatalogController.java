package com.carpass.service_api.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.domain.enums.VehicleKind;
import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.AddressLookupService;
import com.carpass.service_api.service.VehicleCatalogService;
import com.carpass.service_api.web.dto.catalog.AddressLookupResponse;
import com.carpass.service_api.web.dto.catalog.CatalogOption;
import com.carpass.service_api.web.dto.catalog.VehicleCatalogDetail;

/**
 * Consultas de apoio ao preenchimento: CEP e tabela FIPE.
 *
 * <h2>Por que exige login</h2>
 *
 * <p>
 * Os dados em si sao publicos, mas um proxy aberto e um proxy de qualquer um:
 * sem autenticacao, este servidor viraria intermediario gratuito para quem
 * quisesse consultar CEP e FIPE em volume, e a conta -- de reputacao e de
 * bloqueio -- chegaria para a oficina.
 * </p>
 *
 * <h2>Por que login basta, e papel nao</h2>
 *
 * <p>
 * Isto ja exigiu {@code ANY_STAFF}, e a regra estava certa quando toda pessoa
 * autenticada tinha papel numa oficina. Depois da separacao entre identidade e
 * vinculo, deixou de estar: <strong>quem saiu de uma oficina e vai cadastrar a
 * propria nao tem papel nenhum</strong> -- e e exatamente essa pessoa que
 * precisa do CEP para preencher o endereco da oficina nova. Com
 * {@code ANY_STAFF}, o formulario de cadastro respondia "voce nao tem
 * permissao" no meio do preenchimento.
 * </p>
 *
 * <p>
 * Exigir conta continua atendendo ao motivo original: para chegar aqui e
 * preciso ter e-mail cadastrado e senha. O papel nunca foi o que protegia deste
 * risco -- foi so o que estava a mao na epoca.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@PreAuthorize(RoleExpressions.ANY_ACCOUNT)
public class CatalogController {

	private final AddressLookupService addressLookupService;
	private final VehicleCatalogService vehicleCatalogService;

	public CatalogController(AddressLookupService addressLookupService,
			VehicleCatalogService vehicleCatalogService) {
		this.addressLookupService = addressLookupService;
		this.vehicleCatalogService = vehicleCatalogService;
	}

	/**
	 * Endereco a partir do CEP.
	 *
	 * @param zipCode com ou sem mascara; o servico normaliza
	 */
	@GetMapping("/address/{zipCode}")
	public ResponseEntity<AddressLookupResponse> address(@PathVariable String zipCode) {
		return ResponseEntity.ok(addressLookupService.findByZipCode(zipCode));
	}

	// ------------------------------------------------------- veiculos
	@GetMapping("/vehicles/{kind}/brands")
	public ResponseEntity<List<CatalogOption>> brands(@PathVariable VehicleKind kind) {
		return ResponseEntity.ok(vehicleCatalogService.brands(kind));
	}

	@GetMapping("/vehicles/{kind}/models")
	public ResponseEntity<List<CatalogOption>> models(@PathVariable VehicleKind kind,
			@RequestParam String brandCode) {
		return ResponseEntity.ok(vehicleCatalogService.models(kind, brandCode));
	}

	@GetMapping("/vehicles/{kind}/years")
	public ResponseEntity<List<CatalogOption>> years(@PathVariable VehicleKind kind,
			@RequestParam String brandCode, @RequestParam String modelCode) {
		return ResponseEntity.ok(vehicleCatalogService.years(kind, brandCode, modelCode));
	}

	/** O veiculo escolhido, ja com codigo FIPE e combustivel. */
	@GetMapping("/vehicles/{kind}/detail")
	public ResponseEntity<VehicleCatalogDetail> detail(@PathVariable VehicleKind kind,
			@RequestParam String brandCode, @RequestParam String modelCode, @RequestParam String yearCode) {
		return ResponseEntity.ok(vehicleCatalogService.detail(kind, brandCode, modelCode, yearCode));
	}
}
