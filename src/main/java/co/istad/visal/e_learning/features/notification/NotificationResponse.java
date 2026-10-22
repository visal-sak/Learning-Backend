package co.istad.visal.e_learning.features.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String body, String link, boolean read, Instant createdAt) {

    static NotificationResponse of(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt() != null, n.getCreatedAt());
    }
}
