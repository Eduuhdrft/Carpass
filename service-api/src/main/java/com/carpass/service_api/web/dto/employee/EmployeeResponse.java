package com.carpass.service_api.web.dto.employee;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.Employee;

/**
 * Funcionario devolvido pela API.
 *
 * @param hasSystemAccess indica se ha uma conta de login vinculada
 */
public record EmployeeResponse(UUID id, String name, Role role, String roleLabel, String specialty, String phone,
		String email, UUID userId, boolean hasSystemAccess, boolean active, LocalDateTime createdAt) {

	public static EmployeeResponse from(Employee employee) {
		UUID userId = employee.getUser() == null ? null : employee.getUser().getId();
		return new EmployeeResponse(employee.getId(), employee.getName(), employee.getRole(),
				employee.getRole().getLabel(), employee.getSpecialty(), employee.getPhone(), employee.getEmail(),
				userId, userId != null, employee.isActive(), employee.getCreatedAt());
	}
}
