package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserJpaRepository userRepository;

    public Map<String, Object> getPlatformStats() {
        long totalUsers     = userRepository.count();
        long totalExecutors = userRepository.countByRole("EXECUTOR");
        long totalStudents  = userRepository.countByRole("STUDENT");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart  = LocalDate.now().minusDays(7).atStartOfDay();

        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        long newUsersWeek  = userRepository.countByCreatedAtAfter(weekStart);

        // TODO: задания берём из task-service через HTTP или shared DB:
        // long activeTasks    = taskRepository.countByStatus("ACTIVE");
        // long completedTasks = taskRepository.countByStatus("DONE");
        // long disputeTasks   = taskRepository.countByStatus("DISPUTE");
        // double monthRevenue = escrowRepository.sumCompletedThisMonth();
        // double totalRevenue = escrowRepository.sumAllCompleted();
        // double pendingEscrow= escrowRepository.sumPending();

        var result = new LinkedHashMap<String, Object>();
        result.put("totalUsers",     totalUsers);
        result.put("totalExecutors", totalExecutors);
        result.put("totalStudents",  totalStudents);
        result.put("newUsersToday",  newUsersToday);
        result.put("newUsersWeek",   newUsersWeek);
        // Заполнить когда подключите task-service:
        result.put("activeTasks",    0);
        result.put("completedTasks", 0);
        result.put("disputeTasks",   0);
        result.put("totalRevenue",   0.0);
        result.put("monthRevenue",   0.0);
        result.put("pendingEscrow",  0.0);
        return result;
    }

    public List<Map<String, Object>> getRevenue(String period, int limit) {
        // TODO: запрос к payment-service или shared escrow таблице.
        // Возвращаем пустой список — фронт показывает пустой чарт.
        return List.of();
    }

    public Map<String, Object> getTasks(String q, String status, int page, int size) {
        // TODO: HTTP-запрос к task-service GET /api/admin/tasks
        var result = new LinkedHashMap<String, Object>();
        result.put("tasks", List.of());
        result.put("total", 0);
        return result;
    }

    public List<Map<String, Object>> getAntiPlagiatQueue() {
        return userRepository.findAntiPlagiatRequests();
    }
}
