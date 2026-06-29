package ru.studex.user.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SupportMessageRepository {

    private final JdbcTemplate jdbc;

    public record MessageRecord(String role, String content, boolean escalated, String createdAt) {}

    public void save(UUID userId, String role, String content, boolean escalated) {
        jdbc.update(
            "INSERT INTO support_messages (user_id, role, content, escalated) VALUES (?, ?, ?, ?)",
            userId, role, content, escalated
        );
    }

    public void deleteByUser(UUID userId) {
        jdbc.update("DELETE FROM support_messages WHERE user_id = ?", userId);
    }

    public List<MessageRecord> findByUser(UUID userId, int limit) {
        return jdbc.query(
            """
            SELECT role, content, escalated, created_at
            FROM support_messages
            WHERE user_id = ?
            ORDER BY created_at ASC
            LIMIT ?
            """,
            (rs, i) -> new MessageRecord(
                rs.getString("role"),
                rs.getString("content"),
                rs.getBoolean("escalated"),
                rs.getTimestamp("created_at").toInstant().toString()
            ),
            userId, limit
        );
    }
}
