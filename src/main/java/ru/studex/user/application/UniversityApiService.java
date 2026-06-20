package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.UniversityApiDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.UniversityApiKeyEntity;
import ru.studex.user.infrastructure.persistence.UniversityApiKeyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityApiService {

    private final UniversityApiKeyRepository keyRepo;
    private final JdbcTemplate jdbc;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Запрос доступа ────────────────────────────────────────

    @Transactional
    public UniversityApiDtos.AccessResponse requestAccess(UniversityApiDtos.AccessRequest req) {
        if (req.getEmail() == null || !req.getEmail().contains("@"))
            throw new ApiException("Некорректный email", 400);

        if (keyRepo.existsByContactEmailAndStatusNot(req.getEmail(), "REVOKED"))
            throw new ApiException("Заявка с этим email уже существует. Ожидайте подтверждения.", 409);

        UUID universityId = findUniversityId(req.getUniversity());

        UniversityApiKeyEntity entity = UniversityApiKeyEntity.builder()
                .universityId(universityId)
                .universityName(req.getUniversity())
                .apiKey(generateApiKey())
                .contactEmail(req.getEmail())
                .position(req.getPosition())
                .comment(req.getComment())
                .status("PENDING")
                .build();

        keyRepo.save(entity);
        log.info("University API access requested: university={}, email={}", req.getUniversity(), req.getEmail());

        UniversityApiDtos.AccessResponse resp = new UniversityApiDtos.AccessResponse();
        resp.setMessage("Заявка принята. API-ключ придёт на " + req.getEmail() + " в течение 1-2 рабочих дней.");
        resp.setStatus("PENDING");
        return resp;
    }

    // ── Валидация ключа ───────────────────────────────────────

    public UniversityApiKeyEntity validateKey(String apiKey) {
        UniversityApiKeyEntity key = keyRepo.findByApiKeyAndStatus(apiKey, "ACTIVE")
                .orElseThrow(() -> new ApiException("Недействительный или неактивный API-ключ", 401));
        try {
            keyRepo.touchLastUsed(apiKey, LocalDateTime.now());
        } catch (Exception e) {
            log.debug("touchLastUsed failed: {}", e.getMessage());
        }
        return key;
    }

    // ── Основная статистика ───────────────────────────────────

    public UniversityApiDtos.UniversityStatsResponse getStats(String universityName, String period) {
        String interval = periodToInterval(period);

        UniversityApiDtos.UniversityStatsResponse stats = new UniversityApiDtos.UniversityStatsResponse();
        stats.setUniversity(universityName);
        stats.setPeriod(period);

        int total = countTasks(universityName, interval, false);
        stats.setTotalRequests(total);

        int prevTotal = countTasks(universityName, interval, true);
        stats.setTotalRequestsTrend(calcTrend(prevTotal, total));

        BigDecimal avgBudget = safeDecimal(
                "SELECT COALESCE(AVG(COALESCE(t.agreed_price,(t.budget_min+t.budget_max)/2)),0) " +
                        "FROM tasks t JOIN users u ON u.id=t.customer_id " +
                        "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " +
                        "AND t.created_at>=NOW()-INTERVAL '" + interval + "'",
                universityName);
        stats.setAvgBudget(avgBudget.setScale(0, RoundingMode.HALF_UP));

        List<UniversityApiDtos.SubjectStat> subjects = calcTopSubjects(universityName, interval);
        stats.setTopSubjects(subjects);
        stats.setActiveSubjects(subjects.size());

        stats.setCourseYearBreakdown(calcCourseBreakdown(universityName, interval));

        List<UniversityApiDtos.WeekStat> weekly = calcWeeklyActivity(universityName);
        stats.setWeeklyActivity(weekly);
        stats.setPeakWeek(findPeak(weekly));

        return stats;
    }

    // ── Топ предметов ─────────────────────────────────────────

    public UniversityApiDtos.TopSubjectsResponse getTopSubjects(String universityName, String period) {
        String interval = periodToInterval(period);
        UniversityApiDtos.TopSubjectsResponse resp = new UniversityApiDtos.TopSubjectsResponse();
        resp.setUniversity(universityName);
        resp.setPeriod(period);
        resp.setSubjects(calcTopSubjects(universityName, interval));
        return resp;
    }

    // ── Пики активности ───────────────────────────────────────

    public UniversityApiDtos.PeakPeriodsResponse getPeakPeriods(String universityName) {
        List<UniversityApiDtos.WeekStat> weekly = calcWeeklyActivity(universityName);
        UniversityApiDtos.PeakPeriodsResponse resp = new UniversityApiDtos.PeakPeriodsResponse();
        resp.setUniversity(universityName);
        resp.setPeriod("year");
        resp.setWeekly(weekly);
        resp.setPeak(findPeak(weekly));
        return resp;
    }

    // ── Admin ─────────────────────────────────────────────────

    public List<UniversityApiDtos.ApiKeyRecord> listAllKeys() {
        return keyRepo.findAllByOrderByCreatedAtDesc().stream().map(this::toRecord).toList();
    }

    @Transactional
    public UniversityApiDtos.ApiKeyRecord approveKey(String id) {
        UniversityApiKeyEntity key = keyRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ApiException("Запись не найдена", 404));
        key.setStatus("ACTIVE");
        key.setApprovedAt(LocalDateTime.now());
        keyRepo.save(key);
        log.info("University API key approved: id={}, university={}", id, key.getUniversityName());
        return toRecord(key);
    }

    @Transactional
    public UniversityApiDtos.ApiKeyRecord revokeKey(String id) {
        UniversityApiKeyEntity key = keyRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ApiException("Запись не найдена", 404));
        key.setStatus("REVOKED");
        keyRepo.save(key);
        return toRecord(key);
    }

    // ── SQL логика ────────────────────────────────────────────

    private int countTasks(String university, String interval, boolean prev) {
        String timeClause = prev
                ? "AND t.created_at>=NOW()-INTERVAL '" + interval + "' * 2 AND t.created_at<NOW()-INTERVAL '" + interval + "'"
                : "AND t.created_at>=NOW()-INTERVAL '" + interval + "'";
        return safeInt(
                "SELECT COUNT(*) FROM tasks t JOIN users u ON u.id=t.customer_id " +
                        "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " + timeClause,
                university);
    }

    private List<UniversityApiDtos.SubjectStat> calcTopSubjects(String university, String interval) {
        try {
            String sql =
                    "SELECT COALESCE(cat.name,t.subject,'Другое') AS subject, COUNT(*) AS cnt, " +
                            "COALESCE(AVG(COALESCE(t.agreed_price,(t.budget_min+t.budget_max)/2)),0) AS avg_budget " +
                            "FROM tasks t JOIN users u ON u.id=t.customer_id " +
                            "LEFT JOIN categories cat ON cat.id=t.category_id " +
                            "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " +
                            "AND t.created_at>=NOW()-INTERVAL '" + interval + "' " +
                            "GROUP BY subject ORDER BY cnt DESC LIMIT 10";

            List<Map<String, Object>> rows = jdbc.queryForList(sql, university);
            if (rows.isEmpty()) return List.of();

            long maxCnt = ((Number) rows.get(0).get("cnt")).longValue();
            Map<String, Long> prevCounts = calcPrevSubjectCounts(university, interval);

            return rows.stream().map(row -> {
                UniversityApiDtos.SubjectStat s = new UniversityApiDtos.SubjectStat();
                String subj = (String) row.get("subject");
                long cnt = ((Number) row.get("cnt")).longValue();
                s.setSubject(subj);
                s.setCount((int) cnt);
                s.setAvgBudget(new BigDecimal(row.get("avg_budget").toString()).setScale(0, RoundingMode.HALF_UP));
                s.setPct(maxCnt > 0 ? (int)((cnt * 100L) / maxCnt) : 0);
                long prev = prevCounts.getOrDefault(subj, 0L);
                String trend = calcTrend((int) prev, (int) cnt);
                s.setTrend(trend);
                s.setTrendUp(!trend.startsWith("-"));
                return s;
            }).toList();
        } catch (Exception e) {
            log.debug("calcTopSubjects failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Long> calcPrevSubjectCounts(String university, String interval) {
        try {
            String sql =
                    "SELECT COALESCE(cat.name,t.subject,'Другое') AS subject, COUNT(*) AS cnt " +
                            "FROM tasks t JOIN users u ON u.id=t.customer_id " +
                            "LEFT JOIN categories cat ON cat.id=t.category_id " +
                            "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " +
                            "AND t.created_at>=NOW()-INTERVAL '" + interval + "' * 2 " +
                            "AND t.created_at<NOW()-INTERVAL '" + interval + "' " +
                            "GROUP BY subject";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, university);
            Map<String, Long> map = new HashMap<>();
            rows.forEach(r -> map.put((String) r.get("subject"), ((Number) r.get("cnt")).longValue()));
            return map;
        } catch (Exception e) { return Map.of(); }
    }

    private Map<String, Integer> calcCourseBreakdown(String university, String interval) {
        try {
            String sql =
                    "SELECT COALESCE(t.course_year,u.course_year,0) AS cy, COUNT(*) AS cnt " +
                            "FROM tasks t JOIN users u ON u.id=t.customer_id " +
                            "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " +
                            "AND t.created_at>=NOW()-INTERVAL '" + interval + "' " +
                            "AND COALESCE(t.course_year,u.course_year) BETWEEN 1 AND 6 " +
                            "GROUP BY cy ORDER BY cy ASC";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, university);
            Map<String, Integer> map = new LinkedHashMap<>();
            for (int i = 1; i <= 6; i++) map.put(String.valueOf(i), 0);
            rows.forEach(r -> map.put(String.valueOf(((Number)r.get("cy")).intValue()), ((Number)r.get("cnt")).intValue()));
            boolean anyData = map.values().stream().anyMatch(v -> v > 0);
            return anyData ? map : Map.of();
        } catch (Exception e) {
            log.debug("calcCourseBreakdown failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private List<UniversityApiDtos.WeekStat> calcWeeklyActivity(String university) {
        try {
            String sql =
                    "SELECT TO_CHAR(DATE_TRUNC('month',t.created_at),'YYYY-MM') AS ym, COUNT(*) AS cnt " +
                            "FROM tasks t JOIN users u ON u.id=t.customer_id " +
                            "WHERE LOWER(u.university)=LOWER(?) AND t.status!='DRAFT' " +
                            "AND t.created_at>=NOW()-INTERVAL '12 months' " +
                            "GROUP BY ym ORDER BY ym ASC";

            List<Map<String, Object>> rows = jdbc.queryForList(sql, university);
            Map<String, Integer> dataMap = new HashMap<>();
            rows.forEach(r -> dataMap.put((String)r.get("ym"), ((Number)r.get("cnt")).intValue()));

            java.time.YearMonth now = java.time.YearMonth.now();
            List<UniversityApiDtos.WeekStat> result = new ArrayList<>();
            int[] counts = new int[12];

            for (int i = 0; i < 12; i++) {
                java.time.YearMonth ym = now.minusMonths(11 - i);
                int cnt = dataMap.getOrDefault(ym.toString(), 0);
                counts[i] = cnt;
                UniversityApiDtos.WeekStat ws = new UniversityApiDtos.WeekStat();
                ws.setWeek(ym.getMonth().getDisplayName(TextStyle.SHORT, new Locale("ru")));
                ws.setRequests(cnt);
                ws.setLabel("");
                result.add(ws);
            }

            double avg = Arrays.stream(counts).average().orElse(0);
            double threshold = avg * 1.5;
            for (int i = 0; i < result.size(); i++) {
                if (avg > 0 && counts[i] > threshold) result.get(i).setLabel("Сессия");
            }
            return result;
        } catch (Exception e) {
            log.debug("calcWeeklyActivity failed: {}", e.getMessage());
            return List.of();
        }
    }

    private UniversityApiDtos.PeakInfo findPeak(List<UniversityApiDtos.WeekStat> weekly) {
        if (weekly.isEmpty()) return null;
        UniversityApiDtos.WeekStat peak = weekly.stream()
                .max(Comparator.comparingInt(UniversityApiDtos.WeekStat::getRequests))
                .orElse(weekly.get(0));
        UniversityApiDtos.PeakInfo info = new UniversityApiDtos.PeakInfo();
        info.setWeek(peak.getWeek());
        info.setRequests(peak.getRequests());
        info.setLabel(peak.getLabel().isBlank() ? peak.getWeek() : peak.getLabel());
        return info;
    }

    private String periodToInterval(String period) {
        return switch (period == null ? "30d" : period) {
            case "7d"   -> "7 days";
            case "90d"  -> "90 days";
            case "year" -> "1 year";
            default     -> "30 days";
        };
    }

    private String calcTrend(int prev, int curr) {
        if (prev == 0) return curr > 0 ? "+100%" : "0%";
        int diff = (int) Math.round((curr - prev) * 100.0 / prev);
        return (diff >= 0 ? "+" : "") + diff + "%";
    }

    private UUID findUniversityId(String name) {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM universities WHERE LOWER(name)=LOWER(?) LIMIT 1",
                    UUID.class, name);
        } catch (Exception e) { return null; }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("uk_live_");
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private UniversityApiDtos.ApiKeyRecord toRecord(UniversityApiKeyEntity e) {
        UniversityApiDtos.ApiKeyRecord r = new UniversityApiDtos.ApiKeyRecord();
        r.setId(e.getId().toString());
        r.setUniversityName(e.getUniversityName());
        r.setContactEmail(e.getContactEmail());
        r.setPosition(e.getPosition());
        r.setComment(e.getComment());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        r.setApprovedAt(e.getApprovedAt() != null ? e.getApprovedAt().toString() : null);
        r.setLastUsedAt(e.getLastUsedAt() != null ? e.getLastUsedAt().toString() : null);
        if ("ACTIVE".equals(e.getStatus())) r.setApiKey(e.getApiKey());
        return r;
    }

    private int safeInt(String sql, Object... params) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, params);
            return v != null ? v : 0;
        } catch (Exception e) { return 0; }
    }

    private BigDecimal safeDecimal(String sql, Object... params) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, params);
            return v != null ? v : BigDecimal.ZERO;
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
