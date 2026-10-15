package com.carpass.service_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.ExpenseCategory;

/**
 * Categorias de despesa da oficina.
 */
public interface ExpenseCategoryRepository
		extends JpaRepository<ExpenseCategory, UUID>, JpaSpecificationExecutor<ExpenseCategory> {

	Optional<ExpenseCategory> findByIdAndWorkshopId(UUID id, UUID workshopId);

	boolean existsByWorkshopIdAndNameIgnoreCase(UUID workshopId, String name);

	boolean existsByWorkshopIdAndNameIgnoreCaseAndIdNot(UUID workshopId, String name, UUID id);

	/** Para preencher seletor e filtro: um cadastro desativado nao e oferecido. */
	List<ExpenseCategory> findByWorkshopIdAndActiveTrueOrderByNameAsc(UUID workshopId);

	/**
	 * Quantas despesas usam cada categoria.
	 *
	 * <p>
	 * Serve para a tela dizer o que se perde de vista ao desativar. Devolve pares
	 * {@code [idDaCategoria, quantidade]}.
	 * </p>
	 */
	@Query("""
			select e.category.id, count(e)
			from Expense e
			where e.workshop.id = :workshopId
			group by e.category.id
			""")
	List<Object[]> countExpensesByCategory(@Param("workshopId") UUID workshopId);

	@Override
	Page<ExpenseCategory> findAll(Specification<ExpenseCategory> spec, Pageable pageable);
}
