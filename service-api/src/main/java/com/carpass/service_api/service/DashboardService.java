package com.carpass.service_api.service;

import com.carpass.service_api.web.dto.dashboard.DashboardResponse;

/**
 * Numeros da operacao para a tela inicial.
 */
public interface DashboardService {

	DashboardResponse load();
}
