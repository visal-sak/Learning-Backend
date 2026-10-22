package co.istad.visal.e_learning.features.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live connections (Server-Sent Events) per user, so a new notification appears under the bell immediately.
 * A user may have several tabs open; each gets its own emitter.
 */
@Component
public class NotificationHub {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // the browser reconnects after this

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public void push(UUID userId, NotificationResponse notification) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }
    }

    /** Comment line every 25 s keeps proxies from closing idle connections. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        emitters.forEach((user, list) -> list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }));
    }
}
