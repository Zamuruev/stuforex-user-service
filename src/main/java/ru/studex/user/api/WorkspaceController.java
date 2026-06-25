package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.WorkspaceService;

import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/state")
    public ResponseEntity<Map<String, String>> getState(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("state", workspaceService.getState(userId)));
    }

    @PutMapping("/state")
    public ResponseEntity<Void> saveState(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @RequestBody Map<String, String> body) {

        if (userId == null) return ResponseEntity.status(401).build();
        String stateJson = body.get("state");
        if (stateJson == null) return ResponseEntity.badRequest().build();
        workspaceService.saveState(userId, stateJson);
        return ResponseEntity.ok().build();
    }
}
