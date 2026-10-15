package com.carpass.service_api.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.QuoteApprovalToken;

/** Tokens de aprovacao de orcamento pelo cliente. */
public interface QuoteApprovalTokenRepository extends JpaRepository<QuoteApprovalToken, UUID> {

	/**
	 * Localiza pelo hash, ja com a ordem e seus itens carregados.
	 *
	 * <p>
	 * O grafo evita N+1 na tela publica, que precisa listar item a item.
	 * </p>
	 */
	@EntityGraph(attributePaths = { "workOrder", "workOrder.items", "workOrder.customer", "workOrder.vehicle",
			"workOrder.workshop" })
	Optional<QuoteApprovalToken> findByTokenHash(String tokenHash);

	/**
	 * Invalida os links ainda abertos de uma ordem.
	 *
	 * <p>
	 * Chamado antes de emitir um novo: reenviar o orcamento precisa derrubar o
	 * link anterior, senao ficariam dois caminhos validos para decidir sobre a
	 * mesma proposta -- possivelmente com valores diferentes, se os itens mudaram
	 * entre um envio e outro.
	 * </p>
	 */
	@Modifying
	@Query("""
			update QuoteApprovalToken t set t.usedAt = :now
			where t.workOrder.id = :workOrderId and t.usedAt is null
			""")
	void invalidateOpenTokens(@Param("workOrderId") UUID workOrderId, @Param("now") LocalDateTime now);
}
