package ru.studex.user.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.AdDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.application.PartnerAdService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PartnerAdController {

    private final PartnerAdService service;

    // ── Публичный — активные блоки для Маркетплейса ───────────

    @GetMapping("/api/ads")
    public ResponseEntity<List<AdDtos.AdResponse>> getActive() {
        return ResponseEntity.ok(service.getActive());
    }

    // ── Админ — полный CRUD ───────────────────────────────────

    @GetMapping("/api/admin/ads")
    public ResponseEntity<List<AdDtos.AdResponse>> getAll() {
        requireAdmin();
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping("/api/admin/ads")
    public ResponseEntity<AdDtos.AdResponse> create(
            @Valid @RequestBody AdDtos.CreateAdRequest req) {
        requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/api/admin/ads/{id}")
    public ResponseEntity<AdDtos.AdResponse> update(
            @PathVariable UUID id,
            @RequestBody AdDtos.UpdateAdRequest req) {
        requireAdmin();
        return ResponseEntity.ok(service.update(id, req));
    }

    @PostMapping("/api/admin/ads/{id}/toggle")
    public ResponseEntity<AdDtos.AdResponse> toggle(@PathVariable UUID id) {
        requireAdmin();
        return ResponseEntity.ok(service.toggle(id));
    }

    @DeleteMapping("/api/admin/ads/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        requireAdmin();
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────

    private void requireAdmin() {
        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (!isAdmin) throw new ApiException("Доступ запрещён", 403);
    }
}
