package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.service.ExpenseService;
import com.carpass.service_api.service.PartTypeService;
import com.carpass.service_api.web.dto.workshop.UpdateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.WorkshopResponse;

/**
 * Cadastro da oficina, com foco na logo.
 *
 * <p>
 * A validacao de formato e tamanho da imagem existe porque o limite do
 * frontend e conveniencia de interface, nao seguranca: quem chama a API sem
 * passar por aquela tela precisa esbarrar na mesma regra aqui.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class WorkshopServiceImplTest {

	private static final UUID OFICINA = UUID.randomUUID();

	/** PNG minimo (1x1, transparente) -- pequeno o bastante para qualquer limite. */
	private static final String PNG_1X1 = Base64.getEncoder()
			.encodeToString(new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });

	@Mock
	private WorkshopRepository workshopRepository;

	@Mock
	private WorkshopMemberRepository memberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PartTypeService partTypeService;

	@Mock
	private ExpenseService expenseService;

	@Mock
	private FinanceAccess financeAccess;

	@Mock
	private CurrentUser currentUser;

	private WorkshopServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new WorkshopServiceImpl(workshopRepository, memberRepository, userRepository, partTypeService,
				expenseService, financeAccess, currentUser);
		lenient().when(currentUser.requireWorkshopId()).thenReturn(OFICINA);
	}

	@Test
	@DisplayName("logo em formato valido e aceita e guardada como veio")
	void acceptsValidLogo() {
		Workshop oficina = darOficina();
		String logo = "data:image/png;base64," + PNG_1X1;

		WorkshopResponse resposta = service.updateCurrentWorkshop(pedido(logo));

		assertThat(resposta.logoUrl()).isEqualTo(logo);
		assertThat(oficina.getLogoUrl()).isEqualTo(logo);
	}

	@Test
	@DisplayName("campo vazio remove a logo em vez de dar erro")
	void blankLogoClearsIt() {
		Workshop oficina = darOficina();
		oficina.setLogoUrl("data:image/png;base64," + PNG_1X1);

		WorkshopResponse resposta = service.updateCurrentWorkshop(pedido(""));

		assertThat(resposta.logoUrl()).isNull();
	}

	@Test
	@DisplayName("campo nulo remove a logo")
	void nullLogoClearsIt() {
		Workshop oficina = darOficina();
		oficina.setLogoUrl("data:image/png;base64," + PNG_1X1);

		WorkshopResponse resposta = service.updateCurrentWorkshop(pedido(null));

		assertThat(resposta.logoUrl()).isNull();
	}

	@Test
	@DisplayName("texto qualquer que nao e uma data URI de imagem e rejeitado")
	void rejectsNonDataUri() {
		darOficina();

		assertThatThrownBy(() -> service.updateCurrentWorkshop(pedido("https://exemplo.com/logo.png")))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("SVG e rejeitado -- pode carregar script embutido")
	void rejectsSvg() {
		darOficina();
		String svgComoBase64 = Base64.getEncoder()
				.encodeToString("<svg onload='alert(1)'></svg>".getBytes());

		assertThatThrownBy(() -> service.updateCurrentWorkshop(pedido("data:image/svg+xml;base64," + svgComoBase64)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("base64 invalido dentro de uma data URI valida e rejeitado, nao propaga excecao interna")
	void rejectsInvalidBase64Payload() {
		darOficina();

		assertThatThrownBy(() -> service.updateCurrentWorkshop(pedido("data:image/png;base64,!!!nao-e-base64!!!")))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("imagem decodificada acima de 400 KB e rejeitada")
	void rejectsOversizedLogo() {
		darOficina();
		String grande = Base64.getEncoder().encodeToString(new byte[500 * 1024]);

		assertThatThrownBy(() -> service.updateCurrentWorkshop(pedido("data:image/png;base64," + grande)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("400 KB");
	}

	private Workshop darOficina() {
		Workshop oficina = new Workshop();
		oficina.setTradeName("Oficina de Teste");
		when(workshopRepository.findById(OFICINA)).thenReturn(Optional.of(oficina));
		lenient().when(workshopRepository.save(oficina)).thenReturn(oficina);
		return oficina;
	}

	private UpdateWorkshopRequest pedido(String logoUrl) {
		return new UpdateWorkshopRequest("Oficina de Teste", null, null, null, logoUrl, null, null);
	}
}
