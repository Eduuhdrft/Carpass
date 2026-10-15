package com.carpass.service_api.web.dto.expense;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.ExpenseCategory;

/**
 * Categoria de despesa devolvida pela API.
 *
 * @param expenseCount quantos lancamentos usam a categoria, para a tela avisar o
 *                     que sai de vista antes de desativar
 */
public record ExpenseCategoryResponse(UUID id, String name, String description, long expenseCount, boolean active,
		LocalDateTime createdAt) {

	public static ExpenseCategoryResponse from(ExpenseCategory category) {
		return from(category, 0L);
	}

	public static ExpenseCategoryResponse from(ExpenseCategory category, long expenseCount) {
		return new ExpenseCategoryResponse(category.getId(), category.getName(), category.getDescription(),
				expenseCount, category.isActive(), category.getCreatedAt());
	}
}
