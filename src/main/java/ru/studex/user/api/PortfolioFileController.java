package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.studex.user.application.PortfolioFileService;
import ru.studex.user.infrastructure.persistence.PortfolioFileEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio/files")
@RequiredArgsConstructor
public class PortfolioFileController {

    private final PortfolioFileService portfolioFileService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<PortfolioFileEntity>> getFiles(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioFileService.getFiles(userId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PortfolioFileEntity> upload(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "OTHER") String category,
            @RequestParam(value = "description", required = false) String description) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(portfolioFileService.upload(userId, file, category, description));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @PathVariable String fileId) {

        if (userId == null) return ResponseEntity.status(401).build();
        portfolioFileService.delete(userId, fileId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{fileId}")
    public ResponseEntity<PortfolioFileEntity> updateDescription(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId,
            @PathVariable String fileId,
            @RequestBody Map<String, String> body) {

        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(portfolioFileService.updateDescription(userId, fileId, body.get("description")));
    }
}
