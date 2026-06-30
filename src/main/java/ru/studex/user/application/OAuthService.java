package ru.studex.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.config.OAuthProperties;
import ru.studex.user.infrastructure.persistence.*;
import ru.studex.user.infrastructure.security.JwtProvider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthProperties props;
    private final UserJpaRepository userRepository;
    private final UserOAuthProviderRepository oauthProviderRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final XpService xpService;

    private final ConcurrentHashMap<String, PkceEntry> pkceStore = new ConcurrentHashMap<>();

    private record PkceEntry(String codeVerifier, long createdAt) {}

    // ── Public API ────────────────────────────────────────────

    public String buildAuthUrl(String provider) {
        OAuthProperties.ProviderConfig cfg = config(provider);
        String state = randomBase64(24);

        StringBuilder url = new StringBuilder(cfg.getAuthUrl())
                .append("?response_type=code")
                .append("&client_id=").append(enc(cfg.getClientId()))
                .append("&redirect_uri=").append(enc(redirectUri(provider)))
                .append("&scope=").append(enc(cfg.getScope()))
                .append("&state=").append(state);

        if (cfg.isPkce()) {
            String verifier   = randomBase64(64);
            String challenge  = sha256Base64Url(verifier);
            pkceStore.put(state, new PkceEntry(verifier, System.currentTimeMillis()));
            url.append("&code_challenge=").append(challenge)
               .append("&code_challenge_method=S256");
        }

        return url.toString();
    }

    @Transactional
    public String handleCallback(String provider, String code, String state) {
        OAuthProperties.ProviderConfig cfg = config(provider);

        // ── 1. Exchange code → access_token ───────────────────
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "authorization_code");
        form.add("code",          code);
        form.add("redirect_uri",  redirectUri(provider));
        form.add("client_id",     cfg.getClientId());
        form.add("client_secret", cfg.getClientSecret());

        if (cfg.isPkce() && state != null) {
            PkceEntry pkce = pkceStore.remove(state);
            if (pkce != null) form.add("code_verifier", pkce.codeVerifier());
        }

        HttpHeaders formHeaders = new HttpHeaders();
        formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<?, ?> tokenResp;
        try {
            tokenResp = restTemplate.exchange(
                    cfg.getTokenUrl(), HttpMethod.POST,
                    new HttpEntity<>(form, formHeaders), Map.class
            ).getBody();
        } catch (Exception e) {
            log.error("Token exchange failed [{}]: {}", provider, e.getMessage());
            throw new ApiException("Ошибка обмена токена у провайдера", 502);
        }

        String accessToken = (String) tokenResp.get("access_token");
        if (accessToken == null) throw new ApiException("Провайдер не вернул access_token", 502);

        // ── 2. Fetch userinfo ─────────────────────────────────
        HttpHeaders bearerHeaders = new HttpHeaders();
        bearerHeaders.setBearerAuth(accessToken);

        Map<?, ?> userInfo;
        try {
            userInfo = restTemplate.exchange(
                    cfg.getUserinfoUrl(), HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders), Map.class
            ).getBody();
        } catch (Exception e) {
            log.error("UserInfo fetch failed [{}]: {}", provider, e.getMessage());
            throw new ApiException("Не удалось получить профиль от провайдера", 502);
        }

        return findOrCreateUser(provider.toUpperCase(), userInfo);
    }

    // ── Internal ──────────────────────────────────────────────

    private String findOrCreateUser(String provider, Map<?, ?> info) {
        String providerUid = str(info.get("sub"));
        String email       = extractEmail(info);
        String fullName    = extractFullName(info);
        String phone       = extractPhone(info);

        // Already linked
        Optional<UserOAuthProviderEntity> link =
                oauthProviderRepository.findByProviderAndProviderUserId(provider, providerUid);

        UserEntity user;
        if (link.isPresent()) {
            user = userRepository.findById(link.get().getUserId())
                    .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
        } else {
            // Try to merge by email
            Optional<UserEntity> byEmail = email != null
                    ? userRepository.findByEmail(email) : Optional.empty();

            if (byEmail.isPresent()) {
                user = byEmail.get();
            } else {
                String username = generateUsername(fullName, email, provider);
                String placeholderEmail = email != null ? email
                        : provider.toLowerCase() + "_" + providerUid + "@oauth.local";

                user = UserEntity.builder()
                        .email(placeholderEmail)
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .username(username)
                        .fullName(fullName)
                        .phone(phone)
                        .role("STUDENT")
                        .isVerified(true)
                        .build();
                user = userRepository.save(user);

                try {
                    xpService.award(user.getId(), XpEvent.DAILY_LOGIN, user.getId().toString());
                } catch (Exception ignored) {}
            }

            oauthProviderRepository.save(UserOAuthProviderEntity.builder()
                    .userId(user.getId())
                    .provider(provider)
                    .providerUserId(providerUid)
                    .build());
        }

        return jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    private String extractEmail(Map<?, ?> info) {
        Object v = info.get("email");
        return v != null ? str(v) : null;
    }

    private String extractFullName(Map<?, ?> info) {
        if (info.get("name") != null) return str(info.get("name"));
        String given  = info.get("given_name")  != null ? str(info.get("given_name"))  : "";
        String family = info.get("family_name") != null ? str(info.get("family_name")) : "";
        String full   = (given + " " + family).trim();
        return full.isEmpty() ? null : full;
    }

    private String extractPhone(Map<?, ?> info) {
        for (String key : new String[]{"phone_number", "phone", "mobile"}) {
            if (info.get(key) != null) return str(info.get(key));
        }
        return null;
    }

    private String generateUsername(String fullName, String email, String provider) {
        String base;
        if (fullName != null && !fullName.isBlank()) {
            base = fullName.toLowerCase()
                    .replaceAll("[^a-z0-9]", "")
                    .replaceAll("^$", provider.toLowerCase());
        } else if (email != null) {
            base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        } else {
            base = provider.toLowerCase() + "user";
        }
        if (base.length() < 3) base = provider.toLowerCase() + base;
        String stem = base.substring(0, Math.min(base.length(), 14));

        String candidate = stem;
        while (userRepository.existsByUsername(candidate)) {
            candidate = stem + new SecureRandom().nextInt(9000) + 1000;
        }
        return candidate;
    }

    private OAuthProperties.ProviderConfig config(String provider) {
        return switch (provider.toLowerCase()) {
            case "sber"  -> props.getSber();
            case "tbank" -> props.getTbank();
            case "alfa"  -> props.getAlfa();
            default -> throw new ApiException("Неизвестный OAuth-провайдер: " + provider, 400);
        };
    }

    private String redirectUri(String provider) {
        return props.getRedirectBaseUrl() + "/api/auth/oauth/" + provider.toLowerCase() + "/callback";
    }

    private static String str(Object v) { return v != null ? v.toString() : null; }

    private static String enc(String v) {
        return URLEncoder.encode(v != null ? v : "", StandardCharsets.UTF_8);
    }

    private static String randomBase64(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Base64Url(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
