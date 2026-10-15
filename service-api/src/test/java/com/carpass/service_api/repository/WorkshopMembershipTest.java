package com.carpass.service_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * O vinculo entre pessoa e oficina.
 *
 * <p>
 * Estes testes existem por causa de um problema concreto: antes desta tabela,
 * demitir alguem desativava a <strong>identidade</strong> da pessoa. Como o
 * e-mail e unico no sistema inteiro, a pessoa demitida nunca mais conseguia usar
 * o proprio endereco -- nem para abrir a oficina dela, nem para voltar a ser
 * contratada pela mesma oficina. A ex-empregadora ficava, na pratica, com a
 * chave do e-mail pessoal de alguem.
 * </p>
 *
 * <p>
 * O que se cobre aqui e o novo contrato: o que a demissao derruba, o que ela
 * <em>nao</em> pode derrubar, e o isolamento entre oficinas de uma pessoa que
 * pertence a duas.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkshopMembershipTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WorkshopRepository workshopRepository;

	@Autowired
	private WorkshopMemberRepository memberRepository;

	private Workshop oficinaA;
	private Workshop oficinaB;
	private User joao;

	@BeforeEach
	void setUp() {
		oficinaA = criarOficina("Auto Center Silva", "11111111000191", "silva@teste.com");
		oficinaB = criarOficina("Oficina do Bairro", "22222222000192", "bairro@teste.com");
		joao = criarPessoa("joao@gmail.com");
	}

	@Test
	@DisplayName("demitir derruba o vinculo e NAO a identidade")
	void dismissalKeepsIdentityAlive() {
		WorkshopMember vinculo = vincular(joao, oficinaA, Role.MECHANIC);

		vinculo.setActive(false);
		memberRepository.save(vinculo);

		// Esta e a asserção que existe por causa do bug original: se a identidade
		// caisse junto, o e-mail do Joao ficaria preso numa linha que ele nao
		// controla, e ele precisaria de um endereco novo para viver.
		User recarregado = userRepository.findById(joao.getId()).orElseThrow();
		assertThat(recarregado.isActive()).isTrue();
		assertThat(userRepository.findByEmailIgnoreCase("joao@gmail.com")).isPresent();
	}

	@Test
	@DisplayName("demitido nao alcanca mais a oficina, mas continua entrando no sistema")
	void dismissedLosesWorkshopButNotLogin() {
		WorkshopMember vinculo = vincular(joao, oficinaA, Role.MECHANIC);
		vinculo.setActive(false);
		memberRepository.save(vinculo);

		// O que o filtro consulta a cada requisicao: sem vinculo ativo, o token
		// nao autentica em oficina nenhuma.
		assertThat(memberRepository.findByUserIdAndWorkshopIdAndActiveTrue(joao.getId(), oficinaA.getId()))
				.isEmpty();
		assertThat(memberRepository.findActiveByUser(joao.getId())).isEmpty();
	}

	@Test
	@DisplayName("depois de demitido, a mesma pessoa pode ser dona da propria oficina")
	void dismissedCanOwnTheirOwnWorkshop() {
		WorkshopMember antigo = vincular(joao, oficinaA, Role.MECHANIC);
		antigo.setActive(false);
		memberRepository.save(antigo);

		Workshop dele = criarOficina("Oficina do Joao", "33333333000193", "joao-oficina@teste.com");
		vincular(joao, dele, Role.OWNER);

		assertThat(memberRepository.findActiveByUser(joao.getId()))
				.extracting(v -> v.getWorkshop().getTradeName())
				.containsExactly("Oficina do Joao");
	}

	@Test
	@DisplayName("recontratar reativa o vinculo antigo em vez de criar um segundo")
	void rehiringReusesTheSameMembership() {
		WorkshopMember vinculo = vincular(joao, oficinaA, Role.MECHANIC);
		vinculo.setActive(false);
		memberRepository.save(vinculo);

		// O caminho que o grantSystemAccess percorre: acha o vinculo existente,
		// ainda que desativado, e reativa. Duas linhas para o mesmo par pessoa/
		// oficina, com papeis diferentes, nao teriam resposta certa na autorizacao.
		WorkshopMember reencontrado = memberRepository
				.findByUserIdAndWorkshopId(joao.getId(), oficinaA.getId()).orElseThrow();
		assertThat(reencontrado.getId()).isEqualTo(vinculo.getId());

		reencontrado.setRole(Role.MANAGER);
		reencontrado.setActive(true);
		memberRepository.save(reencontrado);

		assertThat(memberRepository.findActiveByUser(joao.getId())).hasSize(1);
	}

	@Test
	@DisplayName("a mesma pessoa pode ter papel diferente em cada oficina")
	void sameIdentityDifferentRolePerWorkshop() {
		vincular(joao, oficinaA, Role.MECHANIC);
		vincular(joao, oficinaB, Role.OWNER);

		assertThat(memberRepository.findByUserIdAndWorkshopIdAndActiveTrue(joao.getId(), oficinaA.getId()))
				.get().extracting(WorkshopMember::getRole).isEqualTo(Role.MECHANIC);
		assertThat(memberRepository.findByUserIdAndWorkshopIdAndActiveTrue(joao.getId(), oficinaB.getId()))
				.get().extracting(WorkshopMember::getRole).isEqualTo(Role.OWNER);
	}

	@Test
	@DisplayName("sair de uma oficina nao mexe no vinculo com a outra")
	void leavingOneWorkshopDoesNotTouchTheOther() {
		WorkshopMember naA = vincular(joao, oficinaA, Role.MECHANIC);
		vincular(joao, oficinaB, Role.OWNER);

		naA.setActive(false);
		memberRepository.save(naA);

		// A oficina A nao pode ter poder sobre o acesso do Joao a oficina B --
		// esse e o isolamento que a tabela de vinculos existe para dar.
		assertThat(memberRepository.findActiveByUser(joao.getId()))
				.extracting(v -> v.getWorkshop().getTradeName())
				.containsExactly("Oficina do Bairro");
	}

	@Test
	@DisplayName("oficina desativada some da lista de escolha")
	void inactiveWorkshopIsNotOffered() {
		vincular(joao, oficinaA, Role.OWNER);
		oficinaA.setActive(false);
		workshopRepository.save(oficinaA);

		// O vinculo continua ativo; quem saiu de cena foi a oficina. Oferecer uma
		// oficina desativada no seletor levaria a uma sessao que nao funciona.
		assertThat(memberRepository.findActiveByUser(joao.getId())).isEmpty();
	}

	@Test
	@DisplayName("a lista de oficinas sai ordenada pelo nome")
	void optionsAreOrderedByName() {
		// Ordem estavel importa: uma lista que muda de ordem entre dois logins
		// faz a pessoa clicar na oficina errada por reflexo.
		Workshop zebra = criarOficina("Zebra Auto", "44444444000194", "zebra@teste.com");
		vincular(joao, zebra, Role.OWNER);
		vincular(joao, oficinaA, Role.MECHANIC);
		vincular(joao, oficinaB, Role.MANAGER);

		assertThat(memberRepository.findActiveByUser(joao.getId()))
				.extracting(v -> v.getWorkshop().getTradeName())
				.containsExactly("Auto Center Silva", "Oficina do Bairro", "Zebra Auto");
	}

	@Test
	@DisplayName("contagem de proprietarios ativos ignora o vinculo desativado")
	void ownerCountIgnoresInactive() {
		// E a trava que impede a oficina de ficar sem dono. Se ela contasse o
		// vinculo desativado, deixaria demitir o ultimo OWNER de verdade.
		User segundoDono = criarPessoa("socio@teste.com");
		WorkshopMember vinculo = vincular(segundoDono, oficinaA, Role.OWNER);

		assertThat(memberRepository.countByWorkshopIdAndRoleAndActiveTrue(oficinaA.getId(), Role.OWNER)).isEqualTo(2);

		vinculo.setActive(false);
		memberRepository.save(vinculo);

		assertThat(memberRepository.countByWorkshopIdAndRoleAndActiveTrue(oficinaA.getId(), Role.OWNER)).isEqualTo(1);
	}

	@Test
	@DisplayName("o vinculo de uma oficina nao vaza para a consulta da outra")
	void membershipDoesNotLeakAcrossWorkshops() {
		vincular(joao, oficinaA, Role.OWNER);

		assertThat(memberRepository.findByUserIdAndWorkshopIdAndActiveTrue(joao.getId(), oficinaB.getId())).isEmpty();
		assertThat(memberRepository.existsByUserIdAndWorkshopIdAndActiveTrue(joao.getId(), oficinaB.getId())).isFalse();
	}

	// ------------------------------------------------------------- fixtures
	private WorkshopMember vincular(User pessoa, Workshop oficina, Role papel) {
		WorkshopMember vinculo = new WorkshopMember();
		vinculo.setUser(pessoa);
		vinculo.setWorkshop(oficina);
		vinculo.setRole(papel);
		vinculo.setActive(true);
		return memberRepository.save(vinculo);
	}

	private User criarPessoa(String email) {
		User pessoa = new User();
		pessoa.setFirstName("Joao");
		pessoa.setLastName("da Silva");
		pessoa.setEmail(email);
		pessoa.setPassword("hash-irrelevante-para-este-teste");
		pessoa.setActive(true);
		return userRepository.save(pessoa);
	}

	/**
	 * Cria uma oficina como a producao cria.
	 *
	 * <p>
	 * O <strong>vinculo</strong> de OWNER do fundador e tao obrigatorio quanto a
	 * coluna {@code workshops.owner_id}: {@code WorkshopService.createFor} grava
	 * os dois, e sao coisas diferentes -- a coluna registra quem fundou, o
	 * vinculo e o que a autorizacao consulta. Uma oficina so com a coluna e uma
	 * oficina que ninguem administra.
	 * </p>
	 *
	 * <p>
	 * Este fixture ja nasceu sem o vinculo, e o teste da contagem de
	 * proprietarios pegou: ele contava um dono onde deveria haver dois. Fixture
	 * menos fiel que a producao e o jeito mais silencioso de um teste passar
	 * enquanto a realidade quebra -- aqui deu sorte de quebrar o teste.
	 * </p>
	 */
	private Workshop criarOficina(String nome, String cnpj, String emailDoDono) {
		User dono = new User();
		dono.setFirstName("Dono");
		dono.setLastName(nome);
		dono.setEmail(emailDoDono);
		dono.setPassword("hash-irrelevante-para-este-teste");
		dono.setActive(true);
		userRepository.save(dono);

		Workshop criada = new Workshop();
		criada.setTradeName(nome);
		criada.setCnpj(cnpj);
		criada.setActive(true);
		criada.setOwner(dono);
		workshopRepository.save(criada);

		vincular(dono, criada, Role.OWNER);
		return criada;
	}
}
