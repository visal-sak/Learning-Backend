package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.expection.ConflictException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-user cap on AI requests (tutor, hints and explanations together), so one client can't run up the bill. */
@Component
public class AiRateLimiter {

    private static final int LIMIT = 30;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<UUID, Deque<Instant>> recent = new ConcurrentHashMap<>();

    public void check(UUID userId) {
        Instant now = Instant.now();
        Deque<Instant> times = recent.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(WINDOW))) times.pollFirst();
            if (times.size() >= LIMIT) {
                throw new ConflictException("You've asked a lot of questions in a short time. Please wait a few minutes.");
            }
            times.addLast(now);
        }
    }
}
