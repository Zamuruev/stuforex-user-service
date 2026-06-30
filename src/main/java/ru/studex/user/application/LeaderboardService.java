package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserJpaRepository userRepository;

    /**
     * Топ по XP — только исполнители.
     */
    public List<Map<String, Object>> byXp(int limit) {
        var users = userRepository.findTopByXp(limit);
        var result = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < users.size(); i++) {
            var u = users.get(i);
            var row = new LinkedHashMap<String, Object>();
            row.put("rank",               i + 1);
            row.put("userId",             u.getId().toString());
            row.put("username",           u.getUsername());
            row.put("fullName",           u.getFullName());

            row.put("xpPoints",           u.getXpPoints());
            row.put("level",              u.getLevel());
            row.put("completedTasks",     0);  // TODO: подтянуть из task-service
            row.put("avgRating",          null);
            row.put("antiPlagiatVerified", Boolean.TRUE.equals(u.getAntiPlagiatVerified()));
            row.put("value",              u.getXpPoints());
            row.put("label",              "XP");
            result.add(row);
        }
        return result;
    }

    /**
     * Топ по выполненным заданиям.
     * Данные берутся из нативного запроса, который считает задания со статусом DONE
     * через поле executor_id в таблице tasks (задания хранятся в task-service БД).
     *
     * Если task-service — отдельная БД, замените нативный запрос на HTTP-вызов.
     */
    public List<Map<String, Object>> byTasks(int limit) {
        var rows = userRepository.findTopByCompletedTasks(limit);
        var result = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < rows.size(); i++) {
            var row = new LinkedHashMap<String, Object>(rows.get(i));
            row.put("rank",  i + 1);
            row.put("label", "заданий");
            row.put("value", row.getOrDefault("completedTasks", 0));
            result.add(row);
        }
        return result;
    }

    /**
     * Топ университетов — группировка по university, суммирование XP.
     */
    public List<Map<String, Object>> byUniversity(int limit) {
        return userRepository.findUniversityStats(limit)
                .stream()
                .map(raw -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("university",   raw.get("university"));
                    row.put("memberCount",  raw.get("member_count"));
                    row.put("totalXp",      raw.get("total_xp"));
                    row.put("totalTasks",   raw.getOrDefault("total_tasks", 0));
                    row.put("avgRating",    raw.getOrDefault("avg_rating", 0.0));
                    return row;
                })
                .collect(Collectors.toList());
    }
}
