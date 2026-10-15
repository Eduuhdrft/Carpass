package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartType;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.repository.projection.ManufacturerSummary;
import com.carpass.service_api.repository.spec.PartSpecifications;

/**
 * Tipo de peca e filtro de marca.
 *
 * <p>
 * O caso de uso que o motivou: o carro esta no elevador, precisa de oleo de
 * motor, e a pergunta e "quais marcas eu tenho?". Antes disso a classificacao
 * era um texto digitado a mao, entao a resposta dependia de todo mundo ter
 * digitado igual.
 * </p>
 *
 * <p>
 * O cenario tem duas oficinas de proposito: um filtro de estoque que atravessa
 * a fronteira da oficina seria o pior tipo de vazamento, porque parece
 * funcionar.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class PartTypeFilterTest {

	@Autowired
	private WorkshopRepository workshopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PartTypeRepository partTypeRepository;

	@Autowired
	private PartRepository partRepository;

	private Workshop oficina;
	private Workshop outraOficina;
	private PartType oleoDeMotor;
	private PartType vela;

	@BeforeEach
	void setUp() {
		oficina = createWorkshop("Oficina de Teste", "11111111000191", "dono@teste.com");
		outraOficina = createWorkshop("Oficina Vizinha", "22222222000192", "vizinho@teste.com");

		oleoDeMotor = createType(oficina, "Oleo de motor");
		vela = createType(oficina, "Vela");
		PartType semTipo = null;

		createPart(oficina, "Oleo 5W30 sintetico", "Mobil", oleoDeMotor, 12);
		createPart(oficina, "Oleo 5W30 sintetico linha leve", "Castrol", oleoDeMotor, 4);
		createPart(oficina, "Oleo 15W40 mineral", "Mobil", oleoDeMotor, 30);
		createPart(oficina, "Vela de ignicao iridium", "NGK", vela, 16);
		createPart(oficina, "Parafuso avulso", "Sem marca definida", semTipo, 100);

		// Mesma marca, oficina diferente: nao pode aparecer para a primeira.
		PartType oleoDaVizinha = createType(outraOficina, "Oleo de motor");
		createPart(outraOficina, "Oleo 5W30 da vizinha", "Mobil", oleoDaVizinha, 99);
	}

	@Test
	@DisplayName("filtrar por tipo traz so as pecas daquele tipo")
	void filtersByType() {
		var filtro = PartSpecifications.filter(oficina.getId(), null, null, false, oleoDeMotor.getId(), null);

		assertThat(partRepository.findAll(filtro, PageRequest.of(0, 20)).getContent())
				.extracting(Part::getName)
				.containsExactlyInAnyOrder("Oleo 5W30 sintetico", "Oleo 5W30 sintetico linha leve",
						"Oleo 15W40 mineral");
	}

	@Test
	@DisplayName("sem filtro de tipo, a peca ainda nao classificada continua na listagem")
	void keepsUntypedPartsWhenNotFilteringByType() {
		var filtro = PartSpecifications.filter(oficina.getId(), null, null, false, null, null);

		assertThat(partRepository.findAll(filtro, PageRequest.of(0, 20)).getContent())
				.extracting(Part::getName)
				.contains("Parafuso avulso");
	}

	@Test
	@DisplayName("tipo e marca juntos estreitam a lista")
	void filtersByTypeAndBrand() {
		var filtro = PartSpecifications.filter(oficina.getId(), null, null, false, oleoDeMotor.getId(), "Mobil");

		assertThat(partRepository.findAll(filtro, PageRequest.of(0, 20)).getContent())
				.extracting(Part::getName)
				.containsExactlyInAnyOrder("Oleo 5W30 sintetico", "Oleo 15W40 mineral");
	}

	@Test
	@DisplayName("a marca casa ignorando maiusculas -- Bosch e BOSCH sao a mesma marca")
	void brandComparisonIgnoresCase() {
		var filtro = PartSpecifications.filter(oficina.getId(), null, null, false, null, "mObIl");

		assertThat(partRepository.findAll(filtro, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
	}

	@Test
	@DisplayName("as marcas de um tipo saem com contagem e saldo")
	void listsManufacturersOfAType() {
		var marcas = partRepository.findManufacturersByType(oficina.getId(), oleoDeMotor.getId());

		assertThat(marcas).extracting(ManufacturerSummary::manufacturer).containsExactly("Castrol", "Mobil");
		assertThat(marcas).filteredOn(m -> m.manufacturer().equals("Mobil")).singleElement()
				.satisfies(m -> {
					assertThat(m.partCount()).isEqualTo(2);
					// 12 + 30: a soma e o que responde "tenho quanto dessa marca?".
					assertThat(m.stockQuantity()).isEqualTo(42);
				});
	}

	@Test
	@DisplayName("as marcas de um tipo nao incluem as de outro tipo")
	void manufacturersAreScopedToTheType() {
		assertThat(partRepository.findManufacturersByType(oficina.getId(), vela.getId()))
				.extracting(ManufacturerSummary::manufacturer)
				.containsExactly("NGK");
	}

	@Test
	@DisplayName("a lista de marcas nao atravessa a fronteira da oficina")
	void manufacturersDoNotLeakAcrossWorkshops() {
		// A vizinha tem uma peca Mobil com 99 em estoque. Se o escopo falhasse,
		// o saldo da Mobil apareceria inflado aqui -- e ninguem notaria.
		assertThat(partRepository.findManufacturers(oficina.getId()))
				.filteredOn(m -> m.manufacturer().equals("Mobil")).singleElement()
				.satisfies(m -> assertThat(m.stockQuantity()).isEqualTo(42));

		assertThat(partRepository.findManufacturers(outraOficina.getId()))
				.extracting(ManufacturerSummary::manufacturer)
				.containsExactly("Mobil");
	}

	@Test
	@DisplayName("filtrar por um tipo de outra oficina nao devolve nada")
	void typeFromAnotherWorkshopReturnsNothing() {
		PartType tipoDaVizinha = partTypeRepository
				.findByWorkshopIdAndActiveTrueOrderByNameAsc(outraOficina.getId()).get(0);

		var filtro = PartSpecifications.filter(oficina.getId(), null, null, false, tipoDaVizinha.getId(), null);

		assertThat(partRepository.findAll(filtro, PageRequest.of(0, 20)).getTotalElements()).isZero();
	}

	@Test
	@DisplayName("o tipo so e alcancavel por id dentro da propria oficina")
	void typeLookupIsScopedToTheWorkshop() {
		assertThat(partTypeRepository.findByIdAndWorkshopId(oleoDeMotor.getId(), oficina.getId())).isPresent();
		assertThat(partTypeRepository.findByIdAndWorkshopId(oleoDeMotor.getId(), outraOficina.getId())).isEmpty();
		assertThat(partTypeRepository.findByIdAndWorkshopId(oleoDeMotor.getId(), UUID.randomUUID())).isEmpty();
	}

	@Test
	@DisplayName("peca inativa sai da lista de marcas")
	void inactivePartsAreNotOffered() {
		Part descontinuada = createPart(oficina, "Oleo descontinuado", "Ipiranga", oleoDeMotor, 5);
		descontinuada.setActive(false);
		partRepository.save(descontinuada);

		assertThat(partRepository.findManufacturersByType(oficina.getId(), oleoDeMotor.getId()))
				.extracting(ManufacturerSummary::manufacturer)
				.doesNotContain("Ipiranga");
	}

	// ------------------------------------------------------------- fixtures
	private Workshop createWorkshop(String name, String cnpj, String ownerEmail) {
		User owner = new User();
		owner.setFirstName("Dono");
		owner.setLastName(name);
		owner.setEmail(ownerEmail);
		owner.setPassword("hash-irrelevante-para-este-teste");
		owner.setActive(true);
		userRepository.save(owner);

		Workshop created = new Workshop();
		created.setTradeName(name);
		created.setCnpj(cnpj);
		created.setActive(true);
		created.setOwner(owner);
		workshopRepository.save(created);

		return created;
	}

	private PartType createType(Workshop owner, String name) {
		PartType type = new PartType();
		type.setWorkshop(owner);
		type.setName(name);
		type.setActive(true);
		return partTypeRepository.save(type);
	}

	private Part createPart(Workshop owner, String name, String manufacturer, PartType type, int stock) {
		Part part = new Part();
		part.setWorkshop(owner);
		part.setName(name);
		part.setManufacturer(manufacturer);
		part.setPartType(type);
		part.setStockQuantity(stock);
		part.setMinimumStock(2);
		part.setActive(true);
		return partRepository.save(part);
	}
}
