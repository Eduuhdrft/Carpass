package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.WorkshopMember;

/**
 * Vinculos entre pessoas e oficinas.
 *
 * <p>
 * E daqui que sai a resposta para "em qual oficina esta pessoa esta operando, e
 * com que papel". Toda a autorizacao depende disso, entao as consultas aqui sao
 * escritas para nao aceitarem vinculo desativado por engano.
 * </p>
 */
public interface WorkshopMemberRepository extends JpaRepository<WorkshopMember, UUID> {

	/**
	 * O vinculo ativo desta pessoa nesta oficina.
	 *
	 * <p>
	 * Chamado a cada requisicao autenticada. E ele que faz a demissao valer na
	 * hora: o token continua assinado e valido, mas sem vinculo ativo por tras
	 * ele nao autentica ninguem.
	 * </p>
	 */
	Optional<WorkshopMember> findByUserIdAndWorkshopIdAndActiveTrue(UUID userId, UUID workshopId);

	/** Inclui o desativado: recontratar reativa a linha existente. */
	Optional<WorkshopMember> findByUserIdAndWorkshopId(UUID userId, UUID workshopId);

	/**
	 * As oficinas em que a pessoa pode entrar hoje.
	 *
	 * <p>
	 * Ordenado pelo nome da oficina para o seletor de troca sair estavel -- uma
	 * lista que muda de ordem entre dois logins faz a pessoa clicar na errada.
	 * O {@code join fetch} evita uma consulta por linha so para ler o nome.
	 * </p>
	 */
	@Query("""
			select m from WorkshopMember m
			join fetch m.workshop w
			where m.user.id = :userId
			  and m.active = true
			  and w.active = true
			order by w.tradeName asc
			""")
	List<WorkshopMember> findActiveByUser(@Param("userId") UUID userId);

	/**
	 * Quantas pessoas ocupam um papel nesta oficina.
	 *
	 * <p>
	 * Existe para impedir que a oficina fique sem dono: rebaixar ou demitir o
	 * ultimo OWNER deixaria a chave do financeiro e do proprio cadastro sem
	 * ninguem que possa mexer.
	 * </p>
	 */
	long countByWorkshopIdAndRoleAndActiveTrue(UUID workshopId, Role role);

	/** O vinculo daquele usuario, para a tela de equipe conferir o que ja existe. */
	boolean existsByUserIdAndWorkshopIdAndActiveTrue(UUID userId, UUID workshopId);
}
