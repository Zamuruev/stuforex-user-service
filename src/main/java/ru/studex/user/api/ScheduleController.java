package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.application.ScheduleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/{userId}/schedule")
    public ResponseEntity<List<Map<String, Object>>> getSchedule(@PathVariable String userId) {
        return ResponseEntity.ok(scheduleService.getSchedule(UUID.fromString(userId)));
    }

    @PutMapping("/{userId}/schedule")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable String userId,
            @AuthenticationPrincipal(errorOnInvalidType = false) String currentUserId,
            @RequestBody Map<String, Object> body) {
        if (currentUserId == null || !currentUserId.equals(userId))
            throw new ApiException("Нет доступа", 403);
        scheduleService.updateSchedule(UUID.fromString(userId), body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/workload")
    public ResponseEntity<Map<String, Object>> getWorkload(@PathVariable String userId) {
        return ResponseEntity.ok(scheduleService.getWorkload(UUID.fromString(userId)));
    }
}
