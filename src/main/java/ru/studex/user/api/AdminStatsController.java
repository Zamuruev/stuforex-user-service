package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.AdminStatsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /** GET /api/admin/stats — сводная статистика платформы */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(adminStatsService.getPlatformStats());
    }

    /** GET /api/admin/revenue?period=month&limit=6 — выручка по периодам */
    @GetMapping("/revenue")
    public ResponseEntity<List<Map<String, Object>>> revenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "6")     int    limit) {
        return ResponseEntity.ok(adminStatsService.getRevenue(period, limit));
    }

    /** GET /api/admin/tasks — все задания с фильтрацией */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> tasks(
            @RequestParam(defaultValue = "")  String q,
            @RequestParam(defaultValue = "")  String status,
            @RequestParam(defaultValue = "0") int    page,
            @RequestParam(defaultValue = "15") int   size) {
        return ResponseEntity.ok(adminStatsService.getTasks(q, status, page, size));
    }

    /** GET /api/admin/anti-plagiat/queue — очередь заявок на антиплагиат */
    @GetMapping("/anti-plagiat/queue")
    public ResponseEntity<List<Map<String, Object>>> antiPlagiatQueue() {
        return ResponseEntity.ok(adminStatsService.getAntiPlagiatQueue());
    }
}
