package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.LeaderboardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * GET /api/leaderboard?type=xp|tasks|university&limit=50
     */
    @GetMapping
    public ResponseEntity<?> leaderboard(
            @RequestParam(defaultValue = "xp")  String type,
            @RequestParam(defaultValue = "50")  int    limit) {
        return switch (type) {
            case "tasks"      -> ResponseEntity.ok(leaderboardService.byTasks(limit));
            case "university" -> ResponseEntity.ok(leaderboardService.byUniversity(limit));
            default           -> ResponseEntity.ok(leaderboardService.byXp(limit));
        };
    }
}
