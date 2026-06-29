package ru.studex.user.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.support.SupportRagService;
import ru.studex.user.application.support.SupportRagService.AnswerResult;
import ru.studex.user.application.support.SupportRagService.ChatMessage;
import ru.studex.user.infrastructure.persistence.SupportMessageRepository;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportChatController {

    private final SupportRagService ragService;
    private final UserJpaRepository userRepository;
    private final SupportMessageRepository messageRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record ChatRequest(
            @NotBlank String message,
            @NotNull List<ChatMessage> history
    ) {}

    public record TicketRequest(
            @NotBlank @Size(max = 255) String subject,
            @NotBlank String message,
            String category,
            String priority
    ) {}

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @Valid @RequestBody ChatRequest req,
            Authentication auth
    ) {
        boolean isPro = resolvePro(auth);
        UUID userId = resolveUserId(auth);

        log.debug("Support chat: isPro={}, message={}", isPro, req.message());
        AnswerResult result = ragService.answer(req.message(), req.history(), isPro);

        if (userId != null) {
            messageRepository.save(userId, "user", req.message(), false);
            messageRepository.save(userId, "assistant", result.reply(), result.escalated());
        }

        return ResponseEntity.ok(Map.of(
            "reply", result.reply(),
            "escalated", result.escalated()
        ));
    }

    @GetMapping("/chat/history")
    public ResponseEntity<Map<String, Object>> history(Authentication auth) {
        UUID userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("messages", List.of()));
        }
        List<SupportMessageRepository.MessageRecord> msgs = messageRepository.findByUser(userId, 100);
        return ResponseEntity.ok(Map.of("messages", msgs));
    }

    @DeleteMapping("/chat/history")
    public ResponseEntity<Void> clearHistory(Authentication auth) {
        UUID userId = resolveUserId(auth);
        if (userId != null) {
            messageRepository.deleteByUser(userId);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ticket")
    public ResponseEntity<Map<String, Object>> createTicket(
            @Valid @RequestBody TicketRequest req,
            Authentication auth
    ) {
        boolean isPro = resolvePro(auth);
        UUID userId = resolveUserId(auth);

        String priority = req.priority() != null ? req.priority() : "normal";
        if ("high".equals(priority) && !isPro) {
            priority = "normal";
        }

        log.info("New support ticket: subject='{}', category={}, priority={}, userId={}",
                req.subject(), req.category(), priority, userId);

        return ResponseEntity.ok(Map.of(
                "status", "created",
                "priority", priority,
                "estimatedResponseTime", isPro ? "5 минут" : "2-4 часа",
                "createdAt", LocalDateTime.now().toString()
        ));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean resolvePro(Authentication auth) {
        UUID userId = resolveUserId(auth);
        if (userId == null) return false;
        try {
            return userRepository.findById(userId)
                    .map(u -> "PRO".equals(u.getSubscriptionPlan()))
                    .orElse(false);
        } catch (Exception e) {
            log.debug("Could not determine subscription: {}", e.getMessage());
            return false;
        }
    }

    private UUID resolveUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        try { return UUID.fromString((String) auth.getPrincipal()); } catch (Exception e) { return null; }
    }
}
