package co.istad.visal.e_learning.features.notification;

import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The bell in the navbar.
 *   GET  /my/notifications              latest notifications
 *   GET  /my/notifications/unread-count
 *   GET  /my/notifications/stream       live updates (Server-Sent Events, event "notification")
 *   POST /my/notifications/{id}/read
 *   POST /my/notifications/read-all
 */
@RestController
@RequestMapping("/api/v1/my/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationHub hub;
    private final CurrentUserService currentUser;

    public NotificationController(NotificationService service, NotificationHub hub, CurrentUserService currentUser) {
        this.service = service;
        this.hub = hub;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationResponse> mine(@RequestParam(defaultValue = "20") int limit) {
        return service.mine(limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", service.unreadCount());
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return hub.connect(currentUser.require().getId());
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable UUID id) {
        service.markRead(id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        service.markAllRead();
    }
}
