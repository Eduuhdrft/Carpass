package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.Workshop;

/**
 * Oficinas cadastradas na plataforma.
 */
public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {

	boolean existsByCnpj(String cnpj);

	Optional<Workshop> findByCnpj(String cnpj);

	/**
	 * Oficinas ativas que ligaram o encerramento automatico.
	 *
	 * <p>
	 * A varredura parte daqui, e nao da tabela de ordens: quem nao ligou a rotina
	 * nao e nem visitado. Com muitas oficinas e poucas com a opcao ligada, isso e
	 * a diferenca entre uma consulta e uma varredura da maior tabela do sistema a
	 * cada hora.
	 * </p>
	 */
	List<Workshop> findByActiveTrueAndAutoCloseOpenOrdersHoursNotNull();
}
