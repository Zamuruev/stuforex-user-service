package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.application.NotificationService;
import ru.studex.user.api.dto.NotificationDtos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDtos.NotificationResponse>> getAll(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        if (userId == null) throw new ApiException("Требуется авторизация", 401);
        return ResponseEntity.ok(notificationService.getForUser(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable String id,
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        if (userId == null) throw new ApiException("Требуется авторизация", 401);
        notificationService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        if (userId == null) throw new ApiException("Требуется авторизация", 401);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        if (userId == null) throw new ApiException("Требуется авторизация", 401);
        notificationService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal")
    public ResponseEntity<Void> createInternal(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String type   = (String) payload.get("type");
        String title  = (String) payload.get("title");
        String body   = (String) payload.get("body");
        String link   = (String) payload.getOrDefault("link", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) payload.getOrDefault("meta", null);

        if (userId == null || type == null || title == null) {
            return ResponseEntity.badRequest().build();
        }

        notificationService.send(UUID.fromString(userId), type, title, body, link, meta);
        return ResponseEntity.ok().build();
    }
}
