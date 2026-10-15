package com.carpass.service_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.repository.projection.MonthlyAmount;

/**
 * Ordens de servico.
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {

	/**
	 * Listagem paginada com filtros dinamicos.
	 *
	 * <p>
	 * O filtro vem de {@code WorkOrderSpecifications}, e nao de uma consulta JPQL
	 * fixa, para que filtro ausente simplesmente nao vire clausula. O motivo esta
	 * documentado naquela classe.
	 * </p>
	 *
	 * <p>
	 * O grafo carrega cliente, veiculo e mecanico junto: a listagem exibe os tres,
	 * e sem isso cada linha dispararia consultas extras.
	 * </p>
	 */
	@Override
	@EntityGraph(attributePaths = { "customer", "vehicle", "assignedMechanic" })
	Page<WorkOrder> findAll(Specification<WorkOrder> spec, Pageable pageable);

	Optional<WorkOrder> findByIdAndWorkshopId(UUID id, UUID workshopId);

	/** Carrega a OS com itens, cliente e veiculo em uma unica consulta (evita N+1). */
	@EntityGraph(attributePaths = { "items", "customer", "vehicle", "assignedMechanic" })
	@Query("select w from WorkOrder w where w.id = :id and w.workshop.id = :workshopId")
	Optional<WorkOrder> findDetailedByIdAndWorkshopId(@Param("id") UUID id, @Param("workshopId") UUID workshopId);

	List<WorkOrder> findByWorkshopIdAndVehicleIdOrderByOpenedAtDesc(UUID workshopId, UUID vehicleId);

	/** Proximo numero sequencial da OS dentro da oficina. */
	@Query("select coalesce(max(w.number), 0) + 1 from WorkOrder w where w.workshop.id = :workshopId")
	Integer nextNumber(@Param("workshopId") UUID workshopId);


	long countByWorkshopIdAndStatus(UUID workshopId, WorkOrderStatus status);

	long countByWorkshopIdAndStatusIn(UUID workshopId, List<WorkOrderStatus> statuses);

	/** Contagem por status, para os cartoes do painel. */
	@Query(value = """
			select w.status, count(w) from WorkOrder w
			where w.workshop.id = :workshopId
			group by w.status
			""")
	List<Object[]> countGroupedByStatus(@Param("workshopId") UUID workshopId);

	/**
	 * Faturamento das OS entregues no periodo.
	 *
	 * <p>
	 * Sem periodo com entregas, a soma vem nula; quem chama trata isso como zero.
	 * Deixamos assim para nao depender de coalesce com literal numerico, que exige
	 * cuidado extra de tipagem no HQL.
	 * </p>
	 */
	@Query(value = """
			select sum(w.totalAmount) from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.status = com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED
			  and w.deliveredAt between :from and :to
			""")
	BigDecimal sumRevenueBetween(@Param("workshopId") UUID workshopId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	/**
	 * O mesmo faturamento, agora quebrado por mes -- e com a contagem de OS.
	 *
	 * <h2>A regra e exatamente a de {@code sumRevenueBetween}</h2>
	 *
	 * <p>
	 * Entra o que foi <strong>entregue</strong>, pela data de entrega. A regra ja
	 * existia no painel e nao muda aqui: se as duas consultas divergissem, o
	 * cartao do painel e a tela financeira mostrariam numeros diferentes para o
	 * mesmo mes, e nao haveria como saber qual esta certo.
	 * </p>
	 *
	 * <p>
	 * Uma chamada cobre o ano inteiro da visao anual e o par "mes escolhido + mes
	 * anterior" da mensal. Mes sem entrega nao aparece no resultado; quem monta a
	 * serie preenche com zero.
	 * </p>
	 */
	@Query(value = """
			select new com.carpass.service_api.repository.projection.MonthlyAmount(
			           year(w.deliveredAt), month(w.deliveredAt), sum(w.totalAmount), count(w))
			from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.status = com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED
			  and w.deliveredAt between :from and :to
			group by year(w.deliveredAt), month(w.deliveredAt)
			order by year(w.deliveredAt), month(w.deliveredAt)
			""")
	List<MonthlyAmount> sumRevenueByMonth(@Param("workshopId") UUID workshopId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/**
	 * Ordens abertas que ninguem toca ha mais tempo que o limite.
	 *
	 * <h2>Por que {@code coalesce(updatedAt, openedAt)}</h2>
	 *
	 * <p>
	 * O relogio conta a partir da <strong>ultima movimentacao</strong>, e nao da
	 * abertura: uma OS mexida ha duas horas nao esta parada, por mais antiga que
	 * seja. O {@code coalesce} cobre o caso da ordem que nasceu e nunca foi
	 * tocada -- justamente a mais provavel de estar abandonada. Sem ele, uma
	 * eventual linha com {@code updated_at} nulo nunca seria alcancada, e a
	 * rotina falharia em silencio exatamente onde deveria agir.
	 * </p>
	 */
	@Query(value = """
			select w from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.status = com.carpass.service_api.domain.enums.WorkOrderStatus.OPEN
			  and coalesce(w.updatedAt, w.openedAt) < :limite
			""")
	List<WorkOrder> findIdleOpenOrders(@Param("workshopId") UUID workshopId,
			@Param("limite") LocalDateTime limite);

	/** Anos em que a oficina entregou alguma OS, do mais recente para o mais antigo. */
	@Query(value = """
			select distinct year(w.deliveredAt) from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.status = com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED
			  and w.deliveredAt is not null
			order by year(w.deliveredAt) desc
			""")
	List<Integer> findYearsWithRevenue(@Param("workshopId") UUID workshopId);

	/** Servicos mais executados, do mais frequente para o menos. */
	@Query(value = """
			select i.description, count(i) as total from WorkOrderItem i
			where i.workOrder.workshop.id = :workshopId
			  and i.type = com.carpass.service_api.domain.enums.ItemType.SERVICE
			group by i.description
			order by total desc
			""")
	List<Object[]> findTopServices(@Param("workshopId") UUID workshopId, Pageable pageable);

	@EntityGraph(attributePaths = { "customer", "vehicle" })
	@Query(value = """
			select w from WorkOrder w
			where w.workshop.id = :workshopId
			order by w.openedAt desc
			""")
	List<WorkOrder> findRecent(@Param("workshopId") UUID workshopId, Pageable pageable);

	/** Ordens com entrega prevista ate a data informada e ainda nao entregues. */
	@EntityGraph(attributePaths = { "customer", "vehicle" })
	@Query("""
			select w from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.estimatedDeliveryDate is not null
			  and w.estimatedDeliveryDate <= :until
			  and w.status not in (com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED,
			                       com.carpass.service_api.domain.enums.WorkOrderStatus.CANCELED)
			order by w.estimatedDeliveryDate asc
			""")
	List<WorkOrder> findUpcomingDeliveries(@Param("workshopId") UUID workshopId, @Param("until") LocalDate until,
			Pageable pageable);

	/** Ordens concluidas de um veiculo, que alimentam a linha do tempo de manutencao. */
	@EntityGraph(attributePaths = { "items", "assignedMechanic" })
	@Query("""
			select w from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.vehicle.id = :vehicleId
			  and w.status in (com.carpass.service_api.domain.enums.WorkOrderStatus.FINISHED,
			                   com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED)
			order by coalesce(w.deliveredAt, w.finishedAt, w.openedAt) desc
			""")
	List<WorkOrder> findCompletedByVehicle(@Param("workshopId") UUID workshopId, @Param("vehicleId") UUID vehicleId);

	/** Total de ordens ainda em aberto na oficina. */
	@Query("""
			select count(w) from WorkOrder w
			where w.workshop.id = :workshopId
			  and w.status not in (com.carpass.service_api.domain.enums.WorkOrderStatus.DELIVERED,
			                       com.carpass.service_api.domain.enums.WorkOrderStatus.CANCELED)
			""")
	long countOpen(@Param("workshopId") UUID workshopId);
}
