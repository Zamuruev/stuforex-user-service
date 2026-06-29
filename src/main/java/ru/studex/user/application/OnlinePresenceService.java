package ru.studex.user.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlinePresenceService {

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    public void recordActivity(String userId) {
        lastSeen.put(userId, Instant.now());
    }

    public int getOnlineCount() {
        Instant cutoff = Instant.now().minus(ONLINE_THRESHOLD);
        return (int) lastSeen.values().stream().filter(t -> t.isAfter(cutoff)).count();
    }

    public boolean isOnline(String userId) {
        Instant last = lastSeen.get(userId);
        return last != null && last.isAfter(Instant.now().minus(ONLINE_THRESHOLD));
    }

    @Scheduled(fixedRate = 120_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(ONLINE_THRESHOLD);
        lastSeen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
