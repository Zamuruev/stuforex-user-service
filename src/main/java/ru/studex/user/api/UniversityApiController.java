package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.UniversityApiDtos;
import ru.studex.user.application.UniversityApiService;
import ru.studex.user.infrastructure.persistence.UniversityApiKeyEntity;

import java.util.List;

/**
 * REST-контроллер Open API для деканатов.
 *
 * Публичные эндпоинты (без ключа):
 *   POST /api/v1/university/request-access   — форма заявки
 *
 * Защищённые эндпоинты (заголовок X-API-Key: uk_live_xxx):
 *   GET  /api/university/stats            — основная статистика
 *   GET  /api/university/subjects         — топ предметов
 *   GET  /api/university/peak-periods     — пики активности
 *
 * Admin-эндпоинты (внутренние, без внешней аутентификации — прикрой на gateway):
 *   GET    /api/university/admin/keys        — все заявки
 *   PATCH  /api/university/admin/keys/{id}/approve — одобрить
 *   PATCH  /api/university/admin/keys/{id}/revoke  — отозвать
 */
@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
public class UniversityApiController {

    private final UniversityApiService service;

    // ── Публичный эндпоинт: заявка на доступ ─────────────────

    @PostMapping("/request-access")
    public ResponseEntity<UniversityApiDtos.AccessResponse> requestAccess(
            @RequestBody UniversityApiDtos.AccessRequest req) {
        return ResponseEntity.ok(service.requestAccess(req));
    }

    // ── Статистика (требует X-API-Key) ────────────────────────

    /**
     * GET /api/university/stats?period=30d
     *
     * period: 7d | 30d | 90d | year (default: 30d)
     */
    @GetMapping("/stats")
    public ResponseEntity<UniversityApiDtos.UniversityStatsResponse> getStats(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(defaultValue = "30d") String period) {

        UniversityApiKeyEntity key = service.validateKey(apiKey);
        return ResponseEntity.ok(service.getStats(key.getUniversityName(), period));
    }

    /**
     * GET /api/v1/university/subjects?period=30d
     * Только топ предметов — удобно для встраивания в дашборды вуза.
     */
    @GetMapping("/subjects")
    public ResponseEntity<UniversityApiDtos.TopSubjectsResponse> getSubjects(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(defaultValue = "30d") String period) {

        UniversityApiKeyEntity key = service.validateKey(apiKey);
        return ResponseEntity.ok(service.getTopSubjects(key.getUniversityName(), period));
    }

    /**
     * GET /api/v1/university/peak-periods
     * Помесячная активность за год — для выявления сессионных пиков.
     */
    @GetMapping("/peak-periods")
    public ResponseEntity<UniversityApiDtos.PeakPeriodsResponse> getPeakPeriods(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        UniversityApiKeyEntity key = service.validateKey(apiKey);
        return ResponseEntity.ok(service.getPeakPeriods(key.getUniversityName()));
    }

    // ── Admin эндпоинты ───────────────────────────────────────

    /** Список всех заявок. Прикрой на api-gateway — только для ADMIN. */
    @GetMapping("/admin/keys")
    public ResponseEntity<List<UniversityApiDtos.ApiKeyRecord>> listKeys() {
        return ResponseEntity.ok(service.listAllKeys());
    }

    /** Одобрить заявку → статус ACTIVE, ключ становится рабочим. */
    @PatchMapping("/admin/keys/{id}/approve")
    public ResponseEntity<UniversityApiDtos.ApiKeyRecord> approveKey(@PathVariable String id) {
        return ResponseEntity.ok(service.approveKey(id));
    }

    /** Отозвать ключ → статус REVOKED, запросы перестают проходить. */
    @PatchMapping("/admin/keys/{id}/revoke")
    public ResponseEntity<UniversityApiDtos.ApiKeyRecord> revokeKey(@PathVariable String id) {
        return ResponseEntity.ok(service.revokeKey(id));
    }
}
