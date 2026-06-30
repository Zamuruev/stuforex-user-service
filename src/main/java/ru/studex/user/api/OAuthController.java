package ru.studex.user.api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.OAuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;

    @Value("${oauth.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Frontend calls this to get the redirect URL, then does window.location.href = url */
    @GetMapping("/{provider}/url")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable String provider) {
        String url = oauthService.buildAuthUrl(provider);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Provider redirects here after user authentication */
    @GetMapping("/{provider}/callback")
    public void callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null || code == null) {
            String msg = URLEncoder.encode(error != null ? error : "no_code", StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/oauth/callback?error=" + msg);
            return;
        }

        try {
            String jwt = oauthService.handleCallback(provider, code, state);
            response.sendRedirect(frontendUrl + "/oauth/callback?token=" + jwt);
        } catch (Exception e) {
            log.error("OAuth callback error [{}]: {}", provider, e.getMessage());
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/oauth/callback?error=" + msg);
        }
    }
}
