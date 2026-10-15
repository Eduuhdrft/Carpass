package com.carpass.service_api.web.dto.workshop;

import com.carpass.service_api.web.dto.common.AddressRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualizacao dos dados da oficina. O CNPJ nao e alteravel: ele identifica a
 * empresa e trocar isso invalidaria o historico.
 */
public record UpdateWorkshopRequest(

		@NotBlank(message = "Informe o nome fantasia")
		@Size(max = 150)
		String tradeName,

		@Size(max = 150)
		String corporateName,

		@Size(max = 30)
		String phone,

		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email,

		/*
		 * A logo, como data URI em base64 (ex.: "data:image/png;base64,...").
		 * String vazia ou nula remove a logo. O limite aqui e so uma barreira
		 * rapida contra payload absurdo; quem valida formato, tipo de imagem e o
		 * tamanho real depois de decodificado e o WorkshopServiceImpl -- checar
		 * isso exige decodificar o base64, o que uma anotacao nao faz sozinha.
		 */
		@Size(max = 600_000, message = "A imagem da logo e grande demais")
		String logoUrl,

		/*
		 * Depois de quantas horas paradas uma OS aberta e encerrada sozinha.
		 * Nulo desliga.
		 *
		 * Vive no formulario da oficina, e nao num endpoint proprio como a chave
		 * do financeiro, porque e decisao operacional e nao de permissao: o
		 * gerente pode mexer sem se conceder acesso a nada.
		 *
		 * O piso de 1 hora impede que um zero digitado por engano encerre tudo
		 * que for aberto; o teto de 720 horas (30 dias) impede um numero absurdo
		 * que na pratica desligaria a rotina sem dizer que desligou. Os mesmos
		 * limites estao no CHECK da tabela.
		 */
		@Min(value = 1, message = "O prazo minimo e de 1 hora")
		@Max(value = 720, message = "O prazo maximo e de 720 horas (30 dias)")
		Integer autoCloseOpenOrdersHours,

		@Valid
		AddressRequest address) {
}
