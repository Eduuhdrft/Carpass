package com.carpass.service_api.web.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.model.Expense;
import com.carpass.service_api.domain.model.User;

/**
 * Despesa devolvida pela API.
 *
 * @param createdBy nome de quem lancou. Vai como texto, e nao so o id, para a
 *                  listagem responder "quem lancou isso?" sem uma segunda busca
 *                  por linha.
 */
public record ExpenseResponse(UUID id, String description, BigDecimal amount, LocalDate expenseDate,
		UUID categoryId, String categoryName, String notes, UUID createdById, String createdBy,
		LocalDateTime createdAt) {

	public static ExpenseResponse from(Expense expense) {
		User author = expense.getCreatedBy();
		return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getAmount(),
				expense.getExpenseDate(), expense.getCategory().getId(), expense.getCategory().getName(),
				expense.getNotes(), author == null ? null : author.getId(),
				author == null ? null : author.getFullName(), expense.getCreatedAt());
	}
}
