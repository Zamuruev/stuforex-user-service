package ru.studex.user.application;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserJpaRepository userRepository;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redis;

    private static final String EMAIL_CODE_PREFIX = "email_verify:";
    private static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final String SITE_URL = "https://stuforex.ru";

    // ── Pre-registration (без аутентификации) ─────────────────

    public void sendPreRegCode(String email) {
        String code = generateCode();
        redis.opsForValue().set(EMAIL_CODE_PREFIX + email.toLowerCase(), code, CODE_TTL);
        sendHtmlEmail(
                email,
                "Код подтверждения Stuforex",
                buildEmailHtml(code, 15)
        );
    }

    public boolean verifyPreRegCode(String email, String code) {
        String key = EMAIL_CODE_PREFIX + email.toLowerCase();
        String stored = redis.opsForValue().get(key);
        if (stored == null || !stored.equals(code)) return false;
        redis.opsForValue().set(key + ":verified", "1", CODE_TTL);
        redis.delete(key);
        return true;
    }

    public boolean isEmailPreVerified(String email) {
        return Boolean.TRUE.equals(redis.hasKey(EMAIL_CODE_PREFIX + email.toLowerCase() + ":verified"));
    }

    public void consumeEmailVerification(String email) {
        redis.delete(EMAIL_CODE_PREFIX + email.toLowerCase() + ":verified");
    }

    // ── Email (после регистрации) ─────────────────────────────

    @Transactional
    public void sendEmailCode(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String code = generateCode();
        user.setEmailCode(code);
        user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        sendHtmlEmail(
                user.getEmail(),
                "Код подтверждения Stuforex",
                buildEmailHtml(code, 15)
        );
    }

    @Transactional
    public boolean confirmEmail(String userId, String code) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (code == null || !code.equals(user.getEmailCode())) return false;
        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) return false;

        user.setEmailVerified(true);
        user.setEmailCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    // ── Phone ─────────────────────────────────────────────────

    @Transactional
    public void sendPhoneCode(String userId, String phone) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }

        String code = generateCode();
        user.setPhoneCode(code);
        user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        log.info("=== SMS VERIFICATION CODE для {} === {}", user.getPhone(), code);
    }

    @Transactional
    public boolean confirmPhone(String userId, String code) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (code == null || !code.equals(user.getPhoneCode())) return false;
        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) return false;

        user.setPhoneVerified(true);
        user.setPhoneCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom("Stuforex <info@stuforex.ru>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Ошибка отправки email на {}: {}", to, e.getMessage());
        }
    }

    /**
     * Собирает HTML-письмо с кодом подтверждения.
     *
     * @param code       шестизначный код
     * @param ttlMinutes время жизни кода в минутах
     */
    private String buildEmailHtml(String code, int ttlMinutes) {
        String formattedCode = code.length() == 6
                ? code.substring(0, 3) + " " + code.substring(3)
                : code;

        return "<!DOCTYPE html><html lang=\"ru\"><head><meta charset=\"UTF-8\"/><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/></head>" +
                "<body style=\"margin:0;padding:0;background:#0a0a14;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#0a0a14;\"><tr><td align=\"center\" style=\"padding:40px 16px;\">" +
                "<div style=\"text-align:center;margin-bottom:28px;\"><span style=\"font-size:26px;font-weight:800;color:#8B6FE8;letter-spacing:-0.03em;\">stuforex</span></div>" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:520px;margin:0 auto;background:#11111f;border:1px solid rgba(139,111,232,0.2);border-radius:20px;overflow:hidden;\">" +
                "<tr><td style=\"background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 52%,#DB6FFF 100%);padding:36px 40px 32px;text-align:center;\">" +
                "<div style=\"width:52px;height:52px;background:rgba(255,255,255,0.18);border-radius:14px;display:inline-block;margin-bottom:16px;line-height:52px;border:1px solid rgba(255,255,255,0.25);\">" +
                "<svg width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"white\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" style=\"vertical-align:middle;\"><rect x=\"2\" y=\"4\" width=\"20\" height=\"16\" rx=\"3\"/><polyline points=\"2,4 12,13 22,4\"/></svg>" +
                "</div>" +
                "<h1 style=\"margin:0 0 6px;font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.02em;\">Подтверждение email</h1>" +
                "<p style=\"margin:0;font-size:13px;color:rgba(255,255,255,0.75);\">Используйте код ниже для завершения регистрации</p>" +
                "</td></tr>" +
                "<tr><td style=\"padding:36px 40px;\">" +
                "<p style=\"margin:0 0 24px;font-size:15px;color:rgba(190,178,255,0.7);line-height:1.6;\">Привет! 👋<br/><br/>Вы указали этот адрес при регистрации на платформе <span style=\"color:rgba(255,255,255,0.92);font-weight:600;\">Stuforex</span>. Введите код ниже в форму на сайте.</p>" +
                "<div style=\"background:rgba(139,111,232,0.08);border:1px solid rgba(110,198,255,0.2);border-radius:14px;padding:28px;text-align:center;margin:0 0 28px;\">" +
                "<div style=\"font-size:11px;font-weight:600;letter-spacing:0.1em;text-transform:uppercase;color:rgba(255,255,255,0.35);margin-bottom:12px;\">Ваш код подтверждения</div>" +
                "<div style=\"font-size:42px;font-weight:800;letter-spacing:0.2em;color:#A78BFA;font-family:'Courier New',Courier,monospace;padding-left:0.2em;\">" + formattedCode + "</div>" +
                "<div style=\"margin-top:10px;font-size:12px;color:rgba(255,255,255,0.3);\">Действителен " + ttlMinutes + " минут</div>" +
                "</div>" +
                "<hr style=\"border:none;border-top:1px solid rgba(110,198,255,0.1);margin:0 0 24px;\"/>" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin-bottom:24px;\"><tr>" +
                "<td width=\"44\" valign=\"top\"><div style=\"width:32px;height:32px;background:rgba(139,111,232,0.1);border:1px solid rgba(110,198,255,0.12);border-radius:9px;text-align:center;line-height:32px;\">" +
                "<svg width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"rgba(110,198,255,0.8)\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" style=\"vertical-align:middle;\"><path d=\"M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z\"/></svg>" +
                "</div></td>" +
                "<td style=\"font-size:13px;color:rgba(190,178,255,0.6);line-height:1.55;\"><span style=\"color:rgba(255,255,255,0.85);font-weight:600;\">Никому не сообщайте этот код.</span><br/>Сотрудники Stuforex никогда не запрашивают коды.</td>" +
                "</tr></table>" +
                "<div style=\"text-align:center;margin:28px 0;\">" +
                "<a href=\"" + SITE_URL + "/register\" style=\"display:inline-block;padding:14px 40px;border-radius:12px;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 50%,#DB6FFF 100%);\">&#x41F;&#x435;&#x440;&#x435;&#x439;&#x442;&#x438; &#x43A; &#x440;&#x435;&#x433;&#x438;&#x441;&#x442;&#x440;&#x430;&#x446;&#x438;&#x438; &rarr;</a>" +
                "</div>" +
                "<div style=\"background:rgba(244,63,94,0.06);border:1px solid rgba(244,63,94,0.18);border-radius:12px;padding:14px 16px;\">" +
                "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td width=\"26\" valign=\"top\">" +
                "<svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"rgba(244,63,94,0.8)\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z\"/><line x1=\"12\" y1=\"9\" x2=\"12\" y2=\"13\"/><line x1=\"12\" y1=\"17\" x2=\"12.01\" y2=\"17\"/></svg>" +
                "</td><td style=\"font-size:12px;color:rgba(244,63,94,0.75);line-height:1.55;\">Если вы не запрашивали код — просто проигнорируйте это письмо.</td></tr></table>" +
                "</div>" +
                "</td></tr></table>" +
                "<div style=\"text-align:center;padding:24px 0 0;\">" +
                "<div style=\"font-size:15px;font-weight:800;color:#8B6FE8;margin-bottom:12px;\">stuforex</div>" +
                "<div style=\"margin-bottom:12px;\">" +
                "<a href=\"" + SITE_URL + "/privacy\" style=\"font-size:11px;color:rgba(110,198,255,0.5);text-decoration:none;margin:0 10px;\">Политика конфиденциальности</a>" +
                "<a href=\"" + SITE_URL + "/help\" style=\"font-size:11px;color:rgba(110,198,255,0.5);text-decoration:none;margin:0 10px;\">Помощь</a>" +
                "</div>" +
                "<p style=\"font-size:11px;color:rgba(255,255,255,0.25);line-height:1.6;margin:0;\">&copy; 2025 Stuforex. Все права защищены.<br/>Это письмо отправлено автоматически — не отвечайте на него.</p>" +
                "</div>" +
                "</td></tr></table>" +
                "</body></html>";
    }

    // DEAD CODE START — delete this entire method if it causes issues
    @SuppressWarnings({"unused","all"})
    private void oldTemplate(String formattedCode, int ttlMinutes) { String styles =
                "@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Syne:wght@700;800&display=swap');" +
                        "*{margin:0;padding:0;box-sizing:border-box}" +
                        "body{background:#080810;font-family:'Inter',-apple-system,BlinkMacSystemFont,sans-serif;-webkit-font-smoothing:antialiased;color:rgba(255,255,255,.92)}" +
                        ".wrap{max-width:560px;margin:0 auto;padding:40px 16px}" +
                        ".logo-block{text-align:center;margin-bottom:32px}" +
                        ".logo{display:inline-block;font-family:'Syne',sans-serif;font-weight:800;font-size:28px;letter-spacing:-.03em;background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 50%,#DB6FFF 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;filter:drop-shadow(0 0 18px rgba(139,111,232,.55))}" +
                        ".card{background:rgba(139,111,232,.05);border:1px solid rgba(110,198,255,.13);border-radius:20px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.35),0 0 40px rgba(139,111,232,.1)}" +
                        ".hero{background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 52%,#DB6FFF 100%);padding:36px 40px 32px;text-align:center;position:relative;overflow:hidden}" +
                        ".hero::before{content:'';position:absolute;inset:0;background:radial-gradient(ellipse at 60% 30%,rgba(255,255,255,.12) 0%,transparent 70%)}" +
                        ".hero-icon{width:56px;height:56px;background:rgba(255,255,255,.18);border-radius:16px;display:inline-flex;align-items:center;justify-content:center;margin-bottom:16px;border:1px solid rgba(255,255,255,.25);position:relative;z-index:1}" +
                        ".hero-icon svg{width:26px;height:26px;fill:none;stroke:#fff;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}" +
                        ".hero h1{font-family:'Syne',sans-serif;font-size:22px;font-weight:800;color:#fff;letter-spacing:-.02em;position:relative;z-index:1;margin-bottom:6px}" +
                        ".hero p{font-size:13px;color:rgba(255,255,255,.75);position:relative;z-index:1}" +
                        ".body{padding:36px 40px}" +
                        ".greeting{font-size:15px;color:rgba(190,178,255,.65);margin-bottom:20px;line-height:1.6}" +
                        ".greeting strong{color:rgba(255,255,255,.92);font-weight:600}" +
                        ".code-block{background:rgba(139,111,232,.07);border:1px solid rgba(110,198,255,.18);border-radius:14px;padding:24px;text-align:center;margin:28px 0}" +
                        ".code-label{font-size:11px;font-weight:600;letter-spacing:.1em;text-transform:uppercase;color:rgba(255,255,255,.35);margin-bottom:10px}" +
                        ".code-value{font-family:'Syne','SF Mono','Fira Code',monospace;font-size:40px;font-weight:800;letter-spacing:.22em;background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 50%,#DB6FFF 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;filter:drop-shadow(0 0 14px rgba(139,111,232,.5));padding-left:.22em}" +
                        ".code-ttl{margin-top:10px;font-size:12px;color:rgba(255,255,255,.3)}" +
                        ".divider{border:none;border-top:1px solid rgba(110,198,255,.10);margin:28px 0}" +
                        ".info-list{display:flex;flex-direction:column;gap:12px;margin-bottom:28px}" +
                        ".info-item{display:flex;align-items:flex-start;gap:12px}" +
                        ".info-icon{width:32px;height:32px;border-radius:9px;background:rgba(139,111,232,.10);border:1px solid rgba(110,198,255,.12);display:flex;align-items:center;justify-content:center;flex-shrink:0;margin-top:1px}" +
                        ".info-icon svg{width:15px;height:15px;stroke:rgba(110,198,255,.8);fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}" +
                        ".info-text{font-size:13px;color:rgba(190,178,255,.6);line-height:1.55}" +
                        ".info-text strong{color:rgba(255,255,255,.85);font-weight:500}" +
                        ".cta-wrap{text-align:center;margin:28px 0}" +
                        ".cta-btn{display:inline-block;padding:14px 40px;border-radius:12px;font-size:14px;font-weight:700;color:#fff;text-decoration:none;background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 50%,#DB6FFF 100%);box-shadow:0 0 28px rgba(139,111,232,.45),0 0 60px rgba(110,198,255,.15)}" +
                        ".warning-box{background:rgba(244,63,94,.06);border:1px solid rgba(244,63,94,.18);border-radius:12px;padding:14px 16px;display:flex;gap:10px;align-items:flex-start}" +
                        ".warning-box svg{width:16px;height:16px;stroke:rgba(244,63,94,.8);fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;flex-shrink:0;margin-top:1px}" +
                        ".warning-box p{font-size:12px;color:rgba(244,63,94,.75);line-height:1.55}" +
                        ".footer{text-align:center;padding:28px 40px 0}" +
                        ".footer-logo{font-family:'Syne',sans-serif;font-size:15px;font-weight:800;background:linear-gradient(135deg,#6EC6FF 0%,#8B6FE8 50%,#DB6FFF 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;margin-bottom:12px}" +
                        ".footer-links{display:flex;justify-content:center;gap:20px;margin:12px 0}" +
                        ".footer-links a{font-size:11px;color:rgba(110,198,255,.5);text-decoration:none}" +
                        ".footer p{font-size:11px;color:rgba(255,255,255,.25);line-height:1.6}" +
                        ".footer a{color:rgba(110,198,255,.55);text-decoration:none}" +
                        "@media(max-width:480px){.hero{padding:28px 24px 24px}.body{padding:28px 24px}.footer{padding:24px 24px 0}.code-value{font-size:30px}}";

        String html = "<!DOCTYPE html>" +
                "<html lang=\"ru\">" +
                "<head>" +
                "<meta charset=\"UTF-8\"/>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>" +
                "<title>Stuforex</title>" +
                "<style>" + styles + "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"wrap\">" +
                "<div class=\"logo-block\"><span class=\"logo\">stuforex</span></div>" +
                "<div class=\"card\">" +
                "<div class=\"hero\">" +
                "<div class=\"hero-icon\">" +
                "<svg viewBox=\"0 0 24 24\"><rect x=\"2\" y=\"4\" width=\"20\" height=\"16\" rx=\"3\"/><polyline points=\"2,4 12,13 22,4\"/></svg>" +
                "</div>" +
                "<h1>\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 email</h1>" +
                "<p>\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 \u043a\u043e\u0434 \u043d\u0438\u0436\u0435 \u0434\u043b\u044f \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0438\u044f \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438</p>" +
                "</div>" +
                "<div class=\"body\">" +
                "<p class=\"greeting\">" +
                "\u041f\u0440\u0438\u0432\u0435\u0442! \ud83d\udc4b<br/><br/>" +
                "\u0412\u044b \u0443\u043a\u0430\u0437\u0430\u043b\u0438 \u044d\u0442\u043e\u0442 \u0430\u0434\u0440\u0435\u0441 \u043f\u0440\u0438 \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438 \u043d\u0430 \u043f\u043b\u0430\u0442\u0444\u043e\u0440\u043c\u0435 <strong>Stuforex</strong>. " +
                "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043a\u043e\u0434 \u043d\u0438\u0436\u0435 \u0432 \u0444\u043e\u0440\u043c\u0443 \u043d\u0430 \u0441\u0430\u0439\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u044c \u0441\u0432\u043e\u044e \u043f\u043e\u0447\u0442\u0443." +
                "</p>" +
                "<div class=\"code-block\">" +
                "<div class=\"code-label\">\u0412\u0430\u0448 \u043a\u043e\u0434 \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u044f</div>" +
                "<div class=\"code-value\">" + formattedCode + "</div>" +
                "<div class=\"code-ttl\">&#128336; \u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0442\u0435\u043b\u0435\u043d " + ttlMinutes + " \u043c\u0438\u043d\u0443\u0442</div>" +
                "</div>" +
                "<hr class=\"divider\"/>" +
                "<div class=\"info-list\">" +
                "<div class=\"info-item\">" +
                "<div class=\"info-icon\">" +
                "<svg viewBox=\"0 0 24 24\"><path d=\"M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z\"/></svg>" +
                "</div>" +
                "<div class=\"info-text\">" +
                "<strong>\u041d\u0438\u043a\u043e\u043c\u0443 \u043d\u0435 \u0441\u043e\u043e\u0431\u0449\u0430\u0439\u0442\u0435 \u044d\u0442\u043e\u0442 \u043a\u043e\u0434.</strong><br/>" +
                "\u0421\u043e\u0442\u0440\u0443\u0434\u043d\u0438\u043a\u0438 Stuforex \u043d\u0438\u043a\u043e\u0433\u0434\u0430 \u043d\u0435 \u0437\u0430\u043f\u0440\u0430\u0448\u0438\u0432\u0430\u044e\u0442 \u043a\u043e\u0434\u044b \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u044f." +
                "</div>" +
                "</div>" +
                "<div class=\"info-item\">" +
                "<div class=\"info-icon\">" +
                "<svg viewBox=\"0 0 24 24\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><line x1=\"12\" y1=\"8\" x2=\"12\" y2=\"12\"/><line x1=\"12\" y1=\"16\" x2=\"12.01\" y2=\"16\"/></svg>" +
                "</div>" +
                "<div class=\"info-text\">" +
                "\u0415\u0441\u043b\u0438 \u0432\u044b \u043d\u0435 \u0441\u043e\u0437\u0434\u0430\u0432\u0430\u043b\u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 \u2014 \u043f\u0440\u043e\u0441\u0442\u043e \u043f\u0440\u043e\u0438\u0433\u043d\u043e\u0440\u0438\u0440\u0443\u0439\u0442\u0435 \u044d\u0442\u043e \u043f\u0438\u0441\u044c\u043c\u043e. \u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043f\u0440\u043e\u0438\u0437\u043e\u0439\u0434\u0451\u0442." +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"cta-wrap\">" +
                "<a href=\"" + SITE_URL + "/register\" class=\"cta-btn\">\u041f\u0435\u0440\u0435\u0439\u0442\u0438 \u043a \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438 &rarr;</a>" +
                "</div>" +
                "<div class=\"warning-box\">" +
                "<svg viewBox=\"0 0 24 24\"><path d=\"M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z\"/><line x1=\"12\" y1=\"9\" x2=\"12\" y2=\"13\"/><line x1=\"12\" y1=\"17\" x2=\"12.01\" y2=\"17\"/></svg>" +
                "<p>\u0415\u0441\u043b\u0438 \u0432\u044b \u043d\u0435 \u0437\u0430\u043f\u0440\u0430\u0448\u0438\u0432\u0430\u043b\u0438 \u043a\u043e\u0434 \u2014 \u043a\u0442\u043e-\u0442\u043e \u043c\u043e\u0433 \u0432\u0432\u0435\u0441\u0442\u0438 \u0432\u0430\u0448 email \u043f\u043e \u043e\u0448\u0438\u0431\u043a\u0435. " +
                "\u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0435\u043c \u0441\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0430\u0440\u043e\u043b\u044c \u043e\u0442 \u043f\u043e\u0447\u0442\u044b, \u0435\u0441\u043b\u0438 \u0441\u043e\u043c\u043d\u0435\u0432\u0430\u0435\u0442\u0435\u0441\u044c \u0432 \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u043e\u0441\u0442\u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0430.</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<div class=\"footer-logo\">stuforex</div>" +
                "<div class=\"footer-links\">" +
                "<a href=\"" + SITE_URL + "/privacy\">\u041f\u043e\u043b\u0438\u0442\u0438\u043a\u0430 \u043a\u043e\u043d\u0444\u0438\u0434\u0435\u043d\u0446\u0438\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u0438</a>" +
                "<a href=\"" + SITE_URL + "/help\">\u041f\u043e\u043c\u043e\u0449\u044c</a>" +
                "</div>" +
                "<p>" +
                "&copy; 2025 Stuforex. \u0412\u0441\u0435 \u043f\u0440\u0430\u0432\u0430 \u0437\u0430\u0449\u0438\u0449\u0435\u043d\u044b.<br/>" +
                "\u042d\u0442\u043e \u043f\u0438\u0441\u044c\u043c\u043e \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e \u0430\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u2014 \u043f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430, \u043d\u0435 \u043e\u0442\u0432\u0435\u0447\u0430\u0439\u0442\u0435 \u043d\u0430 \u043d\u0435\u0433\u043e.<br/>" +
                "<a href=\"mailto:info@stuforex.ru\">info@stuforex.ru</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
        System.out.println(html); // suppress unused warning
    }
}
