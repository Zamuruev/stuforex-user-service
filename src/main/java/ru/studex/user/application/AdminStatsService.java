package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AdminStatsService
 *
 * Агрегирует данные из трёх источников:
 *   1. user-service  — локальный UserJpaRepository
 *   2. task-service  — HTTP GET /api/admin/tasks-stats
 *   3. payment-service — HTTP GET /api/admin/payment-stats
 *
 * Если task-service или payment-service недоступен — логируем и
 * возвращаем нулевые значения, не роняем весь запрос.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserJpaRepository userRepository;
    private final RestClient restClient;

    @Value("${services.task.url:http://localhost:8082}")
    private String taskServiceUrl;

    @Value("${services.payment.url:http://localhost:8084}")
    private String paymentServiceUrl;

    // ── Platform stats ────────────────────────────────────────

    public Map<String, Object> getPlatformStats() {
        // 1. Пользователи из локальной БД
        long totalUsers     = userRepository.count();
        long totalExecutors = userRepository.countByRole("EXECUTOR");
        long totalStudents  = userRepository.countByRole("STUDENT");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart  = LocalDate.now().minusDays(7).atStartOfDay();
        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        long newUsersWeek  = userRepository.countByCreatedAtAfter(weekStart);

        // 2. Задания из task-service
        Map<String, Object> taskStats = fetchTaskStats();

        // 3. Финансы из payment-service
        Map<String, Object> paymentStats = fetchPaymentStats();

        var result = new LinkedHashMap<String, Object>();
        result.put("totalUsers",     totalUsers);
        result.put("totalExecutors", totalExecutors);
        result.put("totalStudents",  totalStudents);
        result.put("newUsersToday",  newUsersToday);
        result.put("newUsersWeek",   newUsersWeek);
        result.put("activeTasks",    taskStats.getOrDefault("activeTasks",    0));
        result.put("completedTasks", taskStats.getOrDefault("completedTasks", 0));
        result.put("disputeTasks",   taskStats.getOrDefault("disputeTasks",   0));
        result.put("totalRevenue",   paymentStats.getOrDefault("totalRevenue",  0.0));
        result.put("monthRevenue",   paymentStats.getOrDefault("monthRevenue",  0.0));
        result.put("pendingEscrow",  paymentStats.getOrDefault("pendingEscrow", 0.0));
        return result;
    }

    // ── Revenue ───────────────────────────────────────────────

    public List<Map<String, Object>> getRevenue(String period, int limit) {
        try {
            var type = new ParameterizedTypeReference<List<Map<String, Object>>>() {};
            List<Map<String, Object>> data = restClient.get()
                    .uri(paymentServiceUrl + "/api/admin/revenue?period={p}&limit={l}", period, limit)
                    .retrieve()
                    .body(type);
            return data != null ? data : List.of();
        } catch (Exception e) {
            log.warn("payment-service /revenue недоступен: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Tasks list (proxied from task-service) ────────────────

    public Map<String, Object> getTasks(String q, String status, int page, int size) {
        try {
            var type = new ParameterizedTypeReference<Map<String, Object>>() {};
            Map<String, Object> data = restClient.get()
                    .uri(taskServiceUrl + "/api/admin/tasks?q={q}&status={s}&page={p}&size={sz}",
                            q, status, page, size)
                    .retrieve()
                    .body(type);
            return data != null ? data : emptyTasksPage();
        } catch (Exception e) {
            log.warn("task-service /admin/tasks недоступен: {}", e.getMessage());
            return emptyTasksPage();
        }
    }

    // ── Anti-plagiat queue ────────────────────────────────────

    public List<Map<String, Object>> getAntiPlagiatQueue() {
        return userRepository.findAntiPlagiatRequests();
    }

    // ── Internal helpers ──────────────────────────────────────

    private Map<String, Object> fetchTaskStats() {
        try {
            var type = new ParameterizedTypeReference<Map<String, Object>>() {};
            Map<String, Object> data = restClient.get()
                    .uri(taskServiceUrl + "/api/admin/tasks-stats")
                    .retrieve()
                    .body(type);
            return data != null ? data : Map.of();
        } catch (Exception e) {
            log.warn("task-service /tasks-stats недоступен: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> fetchPaymentStats() {
        try {
            var type = new ParameterizedTypeReference<Map<String, Object>>() {};
            Map<String, Object> data = restClient.get()
                    .uri(paymentServiceUrl + "/api/admin/payment-stats")
                    .retrieve()
                    .body(type);
            return data != null ? data : Map.of();
        } catch (Exception e) {
            log.warn("payment-service /payment-stats недоступен: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> emptyTasksPage() {
        var m = new LinkedHashMap<String, Object>();
        m.put("tasks", List.of());
        m.put("total", 0);
        return m;
    }
}
