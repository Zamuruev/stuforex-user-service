package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.PortfolioDtos;
import ru.studex.user.application.PortfolioService;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * GET /api/portfolio/{executorId}/cases
     * Публичный: возвращает PUBLIC + LINK кейсы.
     * Владелец: возвращает все включая HIDDEN.
     */
    @GetMapping("/{executorId}/cases")
    public ResponseEntity<List<PortfolioDtos.PortfolioCaseResponse>> getCases(
            @PathVariable String executorId,
            @AuthenticationPrincipal(errorOnInvalidType = false) String requesterId) {

        boolean isOwner = executorId.equals(requesterId);
        return ResponseEntity.ok(portfolioService.getCases(executorId, isOwner));
    }

    /**
     * GET /api/portfolio/{executorId}/stats
     * Агрегированная статистика — публична.
     */
    @GetMapping("/{executorId}/stats")
    public ResponseEntity<PortfolioDtos.PortfolioStatsResponse> getStats(
            @PathVariable String executorId) {

        return ResponseEntity.ok(portfolioService.getStats(executorId));
    }

    /**
     * PATCH /api/portfolio/cases/{caseId}
     * Обновить visibility или ai_summary — только владелец.
     */
    @PatchMapping("/cases/{caseId}")
    public ResponseEntity<PortfolioDtos.PortfolioCaseResponse> updateCase(
            @PathVariable String caseId,
            @AuthenticationPrincipal(errorOnInvalidType = false) String requesterId,
            @RequestBody PortfolioDtos.UpdateCaseRequest req) {

        if (requesterId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(portfolioService.updateCase(caseId, requesterId, req));
    }

    /**
     * POST /api/portfolio/cases/{caseId}/view
     * Инкрементировать счётчик просмотров (fire-and-forget, 200 всегда).
     */
    @PostMapping("/cases/{caseId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable String caseId) {
        portfolioService.incrementView(caseId);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/portfolio/cases/from-task/{taskId}
     * Внутренний — вызывается task-service после completeTask().
     * Создаёт кейс автоматически.
     */
    @PostMapping("/cases/from-task/{taskId}")
    public ResponseEntity<Void> createFromTask(@PathVariable String taskId) {
        portfolioService.createCaseFromTask(taskId);
        return ResponseEntity.ok().build();
    }
}
