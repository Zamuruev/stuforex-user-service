package ru.studex.user.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.application.OnlinePresenceService;
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
    private final OnlinePresenceService presenceService;

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

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {

        if (userId == null) return ResponseEntity.status(401).build();
        userService.changePassword(userId, req.newPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @Valid @RequestBody AuthDtos.DeleteAccountRequest req) {

        if (userId == null) return ResponseEntity.status(401).build();
        userService.deleteAccount(userId, req.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch-avatars")
    public ResponseEntity<Map<String, String>> batchAvatars(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.getOrDefault("ids", List.of());
        return ResponseEntity.ok(userService.getBatchAvatars(ids));
    }

    @PostMapping("/online-check")
    public ResponseEntity<Map<String, Object>> onlineCheck(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.getOrDefault("ids", List.of());
        return ResponseEntity.ok(Map.of("onlineIds", presenceService.areOnline(ids)));
    }

    @GetMapping("/people")
    public ResponseEntity<AuthDtos.AdminUserListResponse> getPeople(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(userService.getPeople(q, page, Math.min(size, 50), isActive));
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

    /** Количество онлайн пользователей (активны последние 5 минут) и всего пользователей */
    @GetMapping("/presence")
    public ResponseEntity<Map<String, Object>> presence() {
        return ResponseEntity.ok(Map.of(
                "onlineCount", presenceService.getOnlineCount(),
                "totalUsers", userService.getTotalUsersCount()
        ));
    }

    /** Обновить план подписки — вызывается payment-service */
    @PatchMapping("/{id}/subscription")
    public ResponseEntity<Void> updateSubscription(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        userService.updateSubscription(id, body.get("subscriptionPlan"), body.get("subscriptionExpiresAt"));
        return ResponseEntity.ok().build();
    }

    /** Загрузить аватар */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDtos.UserResponse> uploadAvatar(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @RequestParam("file") MultipartFile file) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.uploadAvatar(userId, file));
    }

    /** Удалить аватар */
    @DeleteMapping("/me/avatar")
    public ResponseEntity<AuthDtos.UserResponse> deleteAvatar(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.deleteAvatar(userId));
    }

    /** Загрузить обложку профиля */
    @PostMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDtos.UserResponse> uploadCover(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @RequestParam("file") MultipartFile file) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.uploadCover(userId, file));
    }

    /** Удалить обложку профиля */
    @DeleteMapping("/me/cover")
    public ResponseEntity<AuthDtos.UserResponse> deleteCover(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.deleteCover(userId));
    }

    /** Проверить онлайн-статус пользователя */
    @GetMapping("/{id}/online")
    public ResponseEntity<Map<String, Object>> isOnline(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("online", presenceService.isOnline(id)));
    }

    // ── Admin: редактирование чужих профилей ────────────────────────────────

    @PatchMapping("/{id}")
    public ResponseEntity<AuthDtos.UserResponse> adminUpdateProfile(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.updateProfile(id, updates));
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDtos.UserResponse> adminUploadAvatar(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.uploadAvatar(id, file));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<AuthDtos.UserResponse> adminDeleteAvatar(
            @PathVariable String id,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.deleteAvatar(id));
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthDtos.UserResponse> adminUploadCover(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.uploadCover(id, file));
    }

    @DeleteMapping("/{id}/cover")
    public ResponseEntity<AuthDtos.UserResponse> adminDeleteCover(
            @PathVariable String id,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.deleteCover(id));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
