package ru.studex.user.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.studex.user.application.OnlinePresenceService;

@Component
@RequiredArgsConstructor
public class PresenceInterceptor implements HandlerInterceptor {

    private final OnlinePresenceService presenceService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !auth.getName().equals("anonymousUser")) {
            presenceService.recordActivity(auth.getName());
        }
        return true;
    }
}
