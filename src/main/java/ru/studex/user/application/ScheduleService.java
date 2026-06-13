package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.ExecutorScheduleEntity;
import ru.studex.user.infrastructure.persistence.ExecutorScheduleRepository;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ExecutorScheduleRepository scheduleRepository;
    private final UserJpaRepository          userRepository;

    /**
     * Возвращает расписание исполнителя в виде списка слотов.
     * Каждый слот: { dayOfWeek, timeFrom, timeTo }
     */
    public List<Map<String, Object>> getSchedule(UUID userId) {
        return scheduleRepository.findByUserIdOrderByDayOfWeek(userId)
                .stream()
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("dayOfWeek", s.getDayOfWeek());
                    row.put("timeFrom",  s.getTimeFrom().toString());
                    row.put("timeTo",    s.getTimeTo().toString());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * Полная замена расписания: удаляем старое, сохраняем новое.
     * Body: { slots: [ { dayOfWeek, timeFrom, timeTo }, ... ] }
     */
    @Transactional
    public void updateSchedule(UUID userId, Map<String, Object> body) {
        if (!userRepository.existsById(userId))
            throw new ApiException("Пользователь не найден", 404);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots =
                (List<Map<String, Object>>) body.getOrDefault("slots", List.of());

        // Удаляем текущие слоты
        scheduleRepository.deleteByUserId(userId);

        // Сохраняем новые
        for (Map<String, Object> slot : slots) {
            Object dayObj  = slot.get("dayOfWeek");
            Object fromObj = slot.get("timeFrom");
            Object toObj   = slot.get("timeTo");

            if (dayObj == null || fromObj == null || toObj == null) continue;

            int day;
            try { day = Integer.parseInt(dayObj.toString()); }
            catch (NumberFormatException e) { continue; }

            if (day < 1 || day > 7) continue;

            LocalTime timeFrom, timeTo;
            try {
                timeFrom = LocalTime.parse(fromObj.toString());
                timeTo   = LocalTime.parse(toObj.toString());
            } catch (DateTimeParseException e) { continue; }

            if (!timeFrom.isBefore(timeTo)) continue;

            ExecutorScheduleEntity entity = ExecutorScheduleEntity.builder()
                    .userId(userId)
                    .dayOfWeek((short) day)
                    .timeFrom(timeFrom)
                    .timeTo(timeTo)
                    .build();

            scheduleRepository.save(entity);
        }
    }

    /**
     * Загруженность: текущие активные задания / максимум.
     * currentTasks считаем через user_id в task-service — здесь упрощённо через поле в UserEntity.
     * Если интегрируете с task-service через HTTP — замените подсчёт.
     */
    public Map<String, Object> getWorkload(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        int maxConcurrent = user.getMaxConcurrentTasks() != null
                ? user.getMaxConcurrentTasks()
                : 3;

        // TODO: заменить на реальный HTTP-вызов task-service
        // int current = taskServiceClient.countActiveTasksByExecutor(userId.toString());
        int current = 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentTasks",       current);
        result.put("maxConcurrentTasks", maxConcurrent);
        return result;
    }
}
