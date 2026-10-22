package co.istad.visal.e_learning.features.notification;

import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationHub hub;
    private final CurrentUserService currentUser;

    public NotificationService(NotificationRepository repository, NotificationHub hub, CurrentUserService currentUser) {
        this.repository = repository;
        this.hub = hub;
        this.currentUser = currentUser;
    }

    /** Save a notification and push it live to the user's open tabs. */
    @Transactional
    public NotificationResponse notify(UUID userId, String type, String title, String body, String link) {
        NotificationResponse saved = NotificationResponse.of(repository.save(new Notification(userId, type, title, body, link)));
        hub.push(userId, saved);
        return saved;
    }

    public List<NotificationResponse> mine(int limit) {
        UUID me = currentUser.require().getId();
        return repository.findByUserIdOrderByCreatedAtDesc(me, PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(NotificationResponse::of).toList();
    }

    public long unreadCount() {
        return repository.countByUserIdAndReadAtIsNull(currentUser.require().getId());
    }

    @Transactional
    public void markRead(UUID id) {
        UUID me = currentUser.require().getId();
        Notification n = repository.findById(id)
                .filter(x -> x.getUserId().equals(me))
                .orElseThrow(() -> new NotFoundException("No notification with id " + id));
        n.markRead();
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(currentUser.require().getId(), Instant.now());
    }
}
