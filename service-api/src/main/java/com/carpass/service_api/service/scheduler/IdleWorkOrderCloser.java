package com.carpass.service_api.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.carpass.service_api.service.WorkOrderService;

/**
 * Quando a varredura de ordens paradas roda.
 *
 * <h2>Por que so o agendamento mora aqui</h2>
 *
 * <p>
 * A regra -- o que e uma ordem parada e o que fazer com ela -- vive no
 * {@code WorkOrderService}, junto do resto do ciclo de vida da OS. Aqui fica
 * apenas a politica de quando disparar. Separar os dois deixa a regra testavel
 * sem relogio e permite mudar a frequencia sem tocar em nada de negocio.
 * </p>
 *
 * <h2>De hora em hora, e nao de minuto em minuto</h2>
 *
 * <p>
 * O prazo mais curto que a oficina pode configurar e de uma hora, entao varrer
 * com mais frequencia so adiantaria o encerramento em minutos -- e cobraria uma
 * consulta a cada varredura, o dia inteiro. O atraso maximo entre vencer o prazo
 * e ser encerrada e de uma hora, o que e irrelevante para um documento que ja
 * estava parado ha dias.
 * </p>
 *
 * <h2>Uma instancia</h2>
 *
 * <p>
 * Com mais de uma instancia da aplicacao, as duas rodariam a mesma varredura no
 * mesmo minuto. A segunda nao encontraria nada -- a primeira ja teria mudado o
 * status, e a transicao de CANCELED para CANCELED e recusada pela maquina de
 * estados --, mas o desperdicio existiria. Se um dia houver mais de uma
 * instancia, o caminho e um lock distribuido (ShedLock) ou deixar so uma delas
 * com a rotina ligada.
 * </p>
 */
@Component
public class IdleWorkOrderCloser {

	private static final Logger log = LoggerFactory.getLogger(IdleWorkOrderCloser.class);

	/** Uma hora, com o primeiro disparo cinco minutos depois da subida. */
	private static final long INTERVALO_MS = 60L * 60L * 1000L;
	private static final long ATRASO_INICIAL_MS = 5L * 60L * 1000L;

	private final WorkOrderService workOrderService;

	public IdleWorkOrderCloser(WorkOrderService workOrderService) {
		this.workOrderService = workOrderService;
	}

	/**
	 * Varre e encerra.
	 *
	 * <p>
	 * O try/catch existe porque uma excecao nao tratada em metodo agendado faz o
	 * Spring desistir das proximas execucoes em silencio: a rotina simplesmente
	 * pararia de rodar e ninguem saberia por que. Logando e engolindo, um erro de
	 * hoje nao impede a varredura de amanha.
	 * </p>
	 */
	@Scheduled(fixedDelay = INTERVALO_MS, initialDelay = ATRASO_INICIAL_MS)
	public void run() {
		try {
			int encerradas = workOrderService.closeIdleOrders();
			if (encerradas > 0) {
				log.info("Encerramento automatico concluido: {} ordens encerradas.", encerradas);
			}
		} catch (Exception ex) {
			log.error("Falha na varredura de ordens paradas. A proxima execucao segue normalmente.", ex);
		}
	}
}
