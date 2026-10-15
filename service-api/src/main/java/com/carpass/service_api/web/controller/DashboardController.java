package com.carpass.service_api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.DashboardService;
import com.carpass.service_api.web.dto.dashboard.DashboardResponse;

/**
 * Visao geral da operacao, para a tela inicial.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize(RoleExpressions.ANY_STAFF)
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	public ResponseEntity<DashboardResponse> load() {
		return ResponseEntity.ok(dashboardService.load());
	}
}
