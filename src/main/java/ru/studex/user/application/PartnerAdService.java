package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.AdDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.PartnerAdEntity;
import ru.studex.user.infrastructure.persistence.PartnerAdRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerAdService {

    private final PartnerAdRepository repo;

    // ── Публичный — только активные ──────────────────────────

    public List<AdDtos.AdResponse> getActive() {
        return repo.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toResponse).toList();
    }

    // ── Админ — все ───────────────────────────────────────────

    public List<AdDtos.AdResponse> getAll() {
        return repo.findAllByOrderBySortOrderAscCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdDtos.AdResponse create(AdDtos.CreateAdRequest req) {
        PartnerAdEntity e = PartnerAdEntity.builder()
                .company(req.getCompany())
                .tagline(req.getTagline())
                .description(req.getDescription())
                .cta(req.getCta() != null ? req.getCta() : "Подробнее →")
                .url(req.getUrl())
                .accentColor(req.getAccentColor() != null ? req.getAccentColor() : "bg-violet-600 hover:bg-violet-700")
                .bgColor(req.getBgColor()     != null ? req.getBgColor()     : "bg-violet-50 dark:bg-violet-900/10")
                .borderColor(req.getBorderColor() != null ? req.getBorderColor() : "border-violet-200 dark:border-violet-800/50")
                .logo(req.getLogo())
                .active(req.getActive() != null ? req.getActive() : true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        return toResponse(repo.save(e));
    }

    @Transactional
    public AdDtos.AdResponse update(UUID id, AdDtos.UpdateAdRequest req) {
        PartnerAdEntity e = repo.findById(id)
                .orElseThrow(() -> new ApiException("Рекламный блок не найден", 404));
        if (req.getCompany()     != null) e.setCompany(req.getCompany());
        if (req.getTagline()     != null) e.setTagline(req.getTagline());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getCta()         != null) e.setCta(req.getCta());
        if (req.getUrl()         != null) e.setUrl(req.getUrl());
        if (req.getAccentColor() != null) e.setAccentColor(req.getAccentColor());
        if (req.getBgColor()     != null) e.setBgColor(req.getBgColor());
        if (req.getBorderColor() != null) e.setBorderColor(req.getBorderColor());
        if (req.getLogo()        != null) e.setLogo(req.getLogo());
        if (req.getActive()      != null) e.setActive(req.getActive());
        if (req.getSortOrder()   != null) e.setSortOrder(req.getSortOrder());
        return toResponse(repo.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id))
            throw new ApiException("Рекламный блок не найден", 404);
        repo.deleteById(id);
    }

    @Transactional
    public AdDtos.AdResponse toggle(UUID id) {
        PartnerAdEntity e = repo.findById(id)
                .orElseThrow(() -> new ApiException("Рекламный блок не найден", 404));
        e.setActive(!e.getActive());
        return toResponse(repo.save(e));
    }

    // ── Маппер ────────────────────────────────────────────────

    private AdDtos.AdResponse toResponse(PartnerAdEntity e) {
        AdDtos.AdResponse r = new AdDtos.AdResponse();
        r.setId(e.getId());
        r.setCompany(e.getCompany());
        r.setTagline(e.getTagline());
        r.setDescription(e.getDescription());
        r.setCta(e.getCta());
        r.setUrl(e.getUrl());
        r.setAccentColor(e.getAccentColor());
        r.setBgColor(e.getBgColor());
        r.setBorderColor(e.getBorderColor());
        r.setLogo(e.getLogo());
        r.setActive(e.getActive());
        r.setSortOrder(e.getSortOrder());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
