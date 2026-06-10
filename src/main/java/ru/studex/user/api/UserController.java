package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.application.UserService;
import ru.studex.user.application.XpService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final XpService xpService;

    @GetMapping("/{id}/public")
    public ResponseEntity<AuthDtos.UserResponse> getPublicProfile(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, Object> updates) {

        AuthDtos.UserResponse result = userService.updateProfile(userId, updates);

        // XP за заполнение профиля (bio + university/specialization)
        if (updates.containsKey("bio")
                && (updates.containsKey("university") || updates.containsKey("specialization"))) {
            xpService.award(userId, ru.studex.user.infrastructure.persistence.XpEvent.PROFILE_COMPLETED);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuthDtos.UserResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "EXECUTOR") String role) {
        return ResponseEntity.ok(userService.search(q, role));
    }

    @GetMapping("/admin/search")
    public ResponseEntity<AuthDtos.AdminUserListResponse> adminSearch(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        return ResponseEntity.ok(userService.adminSearchUsers(q, role, page, size, isActive));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<AuthDtos.UserResponse> ban(@PathVariable String id) {
        return ResponseEntity.ok(userService.banUser(id));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<AuthDtos.UserResponse> unban(@PathVariable String id) {
        return ResponseEntity.ok(userService.unbanUser(id));
    }

    /**
     * Начислить XP — вызывается другими сервисами (task-service, payment-service).
     * Body: { "event": "TASK_COMPLETED", "contextId": "task-uuid" }
     */
    @PostMapping("/{id}/xp")
    public ResponseEntity<AuthDtos.UserResponse> addXp(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String eventName = body.get("event");
        String contextId = body.getOrDefault("contextId", "global");

        try {
            xpService.award(id, ru.studex.user.infrastructure.persistence.XpEvent.valueOf(eventName), contextId);
        } catch (IllegalArgumentException e) {
            // неизвестное событие — игнорируем, не ломаем вызывающий сервис
        }

        return ResponseEntity.ok(userService.getById(id));
    }

    /**
     * Ежедневный логин — вызывать из AuthController при каждом входе.
     */
    @PostMapping("/me/daily-login")
    public ResponseEntity<Void> dailyLogin(@AuthenticationPrincipal String userId) {
        xpService.award(userId, ru.studex.user.infrastructure.persistence.XpEvent.DAILY_LOGIN, userId);
        return ResponseEntity.ok().build();
    }

    /** Топ исполнители по XP */
    @GetMapping("/top-executors")
    public ResponseEntity<List<AuthDtos.UserResponse>> topExecutors(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(userService.getTopExecutors(limit));
    }

    /** Проверить лимит публикации заданий — вызывается task-service */
    @GetMapping("/{id}/task-limit")
    public ResponseEntity<java.util.Map<String, Object>> getTaskLimit(@PathVariable String id) {
        return ResponseEntity.ok(userService.getTaskLimit(id));
    }

    /** Инкрементировать счётчик заданий — вызывается task-service после создания задания */
    @PostMapping("/{id}/tasks/increment")
    public ResponseEntity<Void> incrementTaskCounter(@PathVariable String id) {
        userService.incrementTaskCounter(id);
        return ResponseEntity.ok().build();
    }
    /** Проверить лимит откликов — вызывается task-service */
    @GetMapping("/{id}/proposal-limit")
    public ResponseEntity<java.util.Map<String, Object>> getProposalLimit(@PathVariable String id) {
        return ResponseEntity.ok(userService.getProposalLimit(id));
    }

    /** Инкрементировать счётчик откликов — вызывается task-service после создания proposal */
    @PostMapping("/{id}/proposals/increment")
    public ResponseEntity<Void> incrementProposalCounter(@PathVariable String id) {
        userService.incrementProposalCounter(id);
        return ResponseEntity.ok().build();
    }

    /** Обновить план подписки — вызывается payment-service */
    @PatchMapping("/{id}/subscription")
    public ResponseEntity<Void> updateSubscription(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        userService.updateSubscription(id, body.get("subscriptionPlan"), body.get("subscriptionExpiresAt"));
        return ResponseEntity.ok().build();
    }
}
