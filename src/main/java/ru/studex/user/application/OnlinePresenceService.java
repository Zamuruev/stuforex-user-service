package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OnlinePresenceService {

    private final StringRedisTemplate redis;

    private static final String PREFIX = "presence:";
    private static final Duration TTL = Duration.ofSeconds(90);

    public void recordActivity(String userId) {
        redis.opsForValue().set(PREFIX + userId, "1", TTL);
    }

    public void removeActivity(String userId) {
        redis.delete(PREFIX + userId);
    }

    public int getOnlineCount() {
        Long count = redis.execute((RedisCallback<Long>) conn -> {
            ScanOptions opts = ScanOptions.scanOptions()
                    .match(PREFIX + "*")
                    .count(200)
                    .build();
            long cnt = 0;
            try (Cursor<byte[]> cursor = conn.scan(opts)) {
                while (cursor.hasNext()) {
                    cursor.next();
                    cnt++;
                }
            }
            return cnt;
        });
        return count != null ? count.intValue() : 0;
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + userId));
    }

    public java.util.Set<String> areOnline(java.util.List<String> userIds) {
        return userIds.stream()
                .filter(id -> Boolean.TRUE.equals(redis.hasKey(PREFIX + id)))
                .collect(java.util.stream.Collectors.toSet());
    }
}
