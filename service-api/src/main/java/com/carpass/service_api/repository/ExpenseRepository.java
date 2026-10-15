package com.carpass.service_api.repository;

import java.time.LocalDate;
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

import com.carpass.service_api.domain.model.Expense;
import com.carpass.service_api.repository.projection.CategoryAmount;
import com.carpass.service_api.repository.projection.MonthlyAmount;

/**
 * Despesas da oficina.
 *
 * <p>
 * Toda soma daqui e feita <strong>no banco</strong>. Carregar as despesas do ano
 * para somar em memoria funcionaria no primeiro mes e ficaria mais lento a cada
 * lancamento -- e o painel financeiro e justamente a tela que envelhece junto
 * com a base.
 * </p>
 */
public interface ExpenseRepository
		extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense>, ExpenseRepositoryCustom {

	@EntityGraph(attributePaths = { "category", "createdBy" })
	Optional<Expense> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByCategoryId(UUID categoryId);

	@Override
	@EntityGraph(attributePaths = { "category", "createdBy" })
	Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

	/** Total de despesas do periodo. Devolve nulo quando nao houve lancamento. */
	@Query("""
			select sum(e.amount) from Expense e
			where e.workshop.id = :workshopId
			  and e.expenseDate between :from and :to
			""")
	java.math.BigDecimal sumBetween(@Param("workshopId") UUID workshopId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/**
	 * Despesas somadas por mes, em uma consulta.
	 *
	 * <p>
	 * Uma chamada cobre o ano inteiro da visao anual e tambem o par
	 * "mes escolhido + mes anterior" da visao mensal. Meses sem lancamento nao
	 * aparecem; quem monta a serie preenche com zero, porque um mes ausente no
	 * grafico e diferente de um mes com zero.
	 * </p>
	 */
	@Query("""
			select new com.carpass.service_api.repository.projection.MonthlyAmount(
			           year(e.expenseDate), month(e.expenseDate), sum(e.amount), count(e))
			from Expense e
			where e.workshop.id = :workshopId
			  and e.expenseDate between :from and :to
			group by year(e.expenseDate), month(e.expenseDate)
			order by year(e.expenseDate), month(e.expenseDate)
			""")
	List<MonthlyAmount> sumByMonth(@Param("workshopId") UUID workshopId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Despesas somadas por categoria dentro do periodo, da maior para a menor. */
	@Query("""
			select new com.carpass.service_api.repository.projection.CategoryAmount(
			           e.category.id, e.category.name, sum(e.amount), count(e))
			from Expense e
			where e.workshop.id = :workshopId
			  and e.expenseDate between :from and :to
			group by e.category.id, e.category.name
			order by sum(e.amount) desc
			""")
	List<CategoryAmount> sumByCategory(@Param("workshopId") UUID workshopId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Anos em que a oficina lancou alguma despesa, do mais recente para o mais antigo. */
	@Query("""
			select distinct year(e.expenseDate) from Expense e
			where e.workshop.id = :workshopId
			order by year(e.expenseDate) desc
			""")
	List<Integer> findYearsWithExpenses(@Param("workshopId") UUID workshopId);
}
