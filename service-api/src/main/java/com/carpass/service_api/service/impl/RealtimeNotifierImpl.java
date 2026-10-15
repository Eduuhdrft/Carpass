package com.carpass.service_api.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.carpass.service_api.config.WebSocketConfig;
import com.carpass.service_api.service.RealtimeNotifier;

/**
 * Envia os avisos pelo canal STOMP.
 *
 * <p>
 * Cada oficina tem um topico proprio, e o id vem sempre de quem chama -- que por
 * sua vez o obteve do usuario autenticado. A assinatura desse topico e checada
 * em {@code WebSocketConfig}: nenhuma tela recebe evento de outra oficina.
 * </p>
 */
@Service
public class RealtimeNotifierImpl implements RealtimeNotifier {

	private static final Logger log = LoggerFactory.getLogger(RealtimeNotifierImpl.class);

	private final SimpMessagingTemplate messagingTemplate;

	public RealtimeNotifierImpl(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void workOrderChanged(UUID workshopId, UUID workOrderId, Integer number) {
		Map<String, Object> payload = base(EventType.WORK_ORDER_CHANGED);
		payload.put("workOrderId", workOrderId);
		payload.put("number", number);
		send(workshopId, payload);
	}

	@Override
	public void quoteDecided(UUID workshopId, UUID workOrderId, Integer number, boolean approved) {
		Map<String, Object> payload = base(EventType.QUOTE_DECIDED);
		payload.put("workOrderId", workOrderId);
		payload.put("number", number);
		payload.put("approved", approved);
		send(workshopId, payload);
	}

	@Override
	public void lowStock(UUID workshopId, UUID partId, String partName, int stockQuantity, int minimumStock) {
		Map<String, Object> payload = base(EventType.LOW_STOCK);
		payload.put("partId", partId);
		// O nome da peca nao e dado sensivel e evita uma busca so para montar o
		// texto do aviso, que precisa ser util sem abrir outra tela.
		payload.put("partName", partName);
		payload.put("stockQuantity", stockQuantity);
		payload.put("minimumStock", minimumStock);
		send(workshopId, payload);
	}

	private Map<String, Object> base(EventType type) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", type.name());
		payload.put("at", LocalDateTime.now().toString());
		return payload;
	}

	/**
	 * Publica no topico da oficina.
	 *
	 * <p>
	 * Envolvido em try/catch de proposito: o aviso e conveniencia, e uma falha
	 * no canal nao pode desfazer uma transacao que ja foi concluida. No pior
	 * caso a tela descobre a mudanca no proximo carregamento.
	 * </p>
	 */
	private void send(UUID workshopId, Map<String, Object> payload) {
		if (workshopId == null) {
			return;
		}
		try {
			messagingTemplate.convertAndSend(WebSocketConfig.WORKSHOP_TOPIC_PREFIX + workshopId, payload);
		} catch (Exception ex) {
			log.warn("Falha ao enviar aviso em tempo real para a oficina {}: {}", workshopId, ex.getMessage());
		}
	}
}
