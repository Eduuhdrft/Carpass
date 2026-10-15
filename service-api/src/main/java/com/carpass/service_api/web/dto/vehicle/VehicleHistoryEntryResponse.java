package com.carpass.service_api.web.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Um evento na linha do tempo de manutencao do veiculo.
 *
 * <p>
 * A linha do tempo junta duas origens: as ordens de servico concluidas nesta
 * oficina e os lancamentos manuais de servicos feitos em outro lugar. O campo
 * {@code source} diz de onde veio cada evento, para a interface poder diferenciar
 * o que a oficina executou do que apenas registrou.
 * </p>
 *
 * @param source       {@code WORK_ORDER} ou {@code MANUAL}
 * @param workOrderId  preenchido apenas quando a origem e uma ordem de servico
 * @param services     descricao dos servicos executados
 * @param parts        descricao das pecas aplicadas
 */
public record VehicleHistoryEntryResponse(String source, UUID id, UUID workOrderId, Integer workOrderNumber,
		LocalDate date, Integer mileage, String description, List<String> services, List<String> parts,
		String performedBy, BigDecimal amount, String notes) {
}
