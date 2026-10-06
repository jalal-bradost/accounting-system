package com.bradox.erp.inventory.service.domain;

import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.AvailabilityChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process SSE hub for warehouse availability invalidation (single-node).
 * Emitters are keyed by {@code companyId:warehouseId}.
 */
@Component
public class StockHoldAvailabilityHub {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID companyId, UUID warehouseId) {
        String key = key(companyId, warehouseId);
        // Finite timeout so dead clients release Tomcat threads; browser reconnects via EventSource/fetch loop.
        SseEmitter emitter = new SseEmitter(15 * 60_000L);
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> {
            remove(key, emitter);
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitter.onError(e -> remove(key, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(key, emitter);
        }
        return emitter;
    }

    public void publish(AvailabilityChangedEvent event) {
        if (event == null || event.warehouseId() == null) return;
        String key = key(event.companyId(), event.warehouseId());
        List<SseEmitter> list = emitters.get(key);
        if (list == null || list.isEmpty()) return;
        Map<String, Object> payload = Map.of(
                "warehouseId", event.warehouseId().toString(),
                "productIds", event.productIds() == null ? List.of()
                        : event.productIds().stream().map(UUID::toString).toList());
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("availability-changed").data(payload));
            } catch (Exception e) {
                remove(key, emitter);
                try { emitter.complete(); } catch (Exception ignored) {}
            }
        }
    }

    private void remove(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(key, list);
        }
    }

    private static String key(UUID companyId, UUID warehouseId) {
        return companyId + ":" + warehouseId;
    }
}
