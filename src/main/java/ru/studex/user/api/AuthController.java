package ru.studex.user.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.application.AuthService;
import ru.studex.user.application.OnlinePresenceService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OnlinePresenceService presenceService;

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {
        log.info("Register attempt: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());
        // XP начисляется внутри authService.login() до формирования ответа
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> me(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    /** Heartbeat — продлевает TTL присутствия в Redis */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal String userId) {
        presenceService.recordActivity(userId);
        return ResponseEntity.ok().build();
    }

    /** Logout — мгновенно убирает пользователя из онлайна */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        if (userId != null) presenceService.removeActivity(userId);
        return ResponseEntity.ok().build();
    }
}
