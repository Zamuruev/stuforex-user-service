package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.NotificationDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.NotificationEntity;
import ru.studex.user.infrastructure.persistence.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate  messagingTemplate;

    // ── Public API (вызывается из контроллера) ────────────────

    public List<NotificationDtos.NotificationResponse> getForUser(String userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markRead(String notifId, String userId) {
        NotificationEntity n = notificationRepository.findById(UUID.fromString(notifId))
                .orElseThrow(() -> new ApiException("Уведомление не найдено", 404));
        if (!n.getUserId().toString().equals(userId))
            throw new ApiException("Нет доступа", 403);
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(String userId) {
        notificationRepository.markAllRead(UUID.fromString(userId));
    }

    @Transactional
    public void delete(String notifId, String userId) {
        NotificationEntity n = notificationRepository.findById(UUID.fromString(notifId))
                .orElseThrow(() -> new ApiException("Уведомление не найдено", 404));
        if (!n.getUserId().toString().equals(userId))
            throw new ApiException("Нет доступа", 403);
        notificationRepository.delete(n);
    }

    // ── Internal: создать и отправить в реалтайме ─────────────

    /**
     * Создаёт уведомление в БД и пушит через WebSocket.
     *
     * @param userId   получатель
     * @param type     тип (PROPOSAL_ACCEPTED, NEW_MESSAGE, …)
     * @param title    заголовок уведомления
     * @param body     текст уведомления
     * @param link     URL для перехода при клике (может быть null)
     * @param meta     доп. данные: taskId, taskTitle, fromUsername, amount, …
     */
    @Transactional
    public NotificationEntity send(
            UUID userId,
            String type,
            String title,
            String body,
            String link,
            Map<String, Object> meta
    ) {
        NotificationEntity n = NotificationEntity.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .meta(meta)
                .build();
        n = notificationRepository.save(n);

        // Реалтайм пуш через WebSocket/STOMP
        try {
            messagingTemplate.convertAndSend(
                    "/topic/notifications." + userId,
                    toDto(n)
            );
        } catch (Exception e) {
            log.warn("Не удалось отправить WebSocket-уведомление userId={}: {}", userId, e.getMessage());
        }

        log.debug("Notification sent: type={}, userId={}", type, userId);
        return n;
    }

    // ── Конкретные события ────────────────────────────────────

    /** Исполнитель выбран по заданию */
    public void notifyExecutorSelected(
            UUID executorId, String taskTitle, String taskId, String customerUsername
    ) {
        send(
                executorId,
                "PROPOSAL_ACCEPTED",
                "Вас выбрали исполнителем!",
                "«" + taskTitle + "» — " + customerUsername + " выбрал вас для выполнения задания",
                "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle, "fromUsername", customerUsername)
        );
    }

    /** Студент открыл спор */
    public void notifyDisputeOpened(
            UUID executorId, UUID customerId, String taskTitle, String taskId
    ) {
        String msg = "По заданию «" + taskTitle + "» открыт спор. Ожидайте решения модератора";
        send(executorId, "DISPUTE_OPENED", "Открыт спор", msg, "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle));
        send(customerId, "DISPUTE_OPENED", "Спор открыт", msg, "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle));
    }

    /** Буст задания истёк */
    public void notifyBoostExpired(UUID customerId, String taskTitle, String taskId) {
        send(
                customerId,
                "TASK_COMPLETED", // переиспользуем тип, фронт покажет нейтральную иконку
                "Буст завершён",
                "Продвижение задания «" + taskTitle + "» закончилось. Продлите для большей видимости",
                "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle)
        );
    }

    /** Подписка истекает через 3 дня */
    public void notifySubscriptionExpiring(UUID userId, String planName, String expiresAt) {
        send(
                userId,
                "SUBSCRIPTION_EXPIRING",
                "Подписка скоро закончится",
                "Ваш план " + planName + " истекает " + expiresAt + ". Продлите чтобы не потерять доступ",
                "/profile/" + userId,
                Map.of("plan", planName, "expiresAt", expiresAt)
        );
    }

    /** Исполнитель получил аванс */
    public void notifyAdvancePaid(UUID executorId, String taskTitle, String taskId, String amount) {
        send(
                executorId,
                "ADVANCE_PAID",
                "Аванс зачислен",
                "По заданию «" + taskTitle + "» получен аванс " + amount + " ₽",
                "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle, "amount", amount)
        );
    }

    /** Исполнитель получил финальную выплату */
    public void notifyPaymentReceived(UUID executorId, String taskTitle, String taskId, String amount) {
        send(
                executorId,
                "PAYMENT_RECEIVED",
                "Оплата получена",
                "По заданию «" + taskTitle + "» зачислено " + amount + " ₽",
                "/marketplace/" + taskId,
                Map.of("taskId", taskId, "taskTitle", taskTitle, "amount", amount)
        );
    }

    /** Новое сообщение в чате */
    public void notifyNewMessage(
            UUID recipientId, String senderUsername, String roomId, String taskTitle
    ) {
        send(
                recipientId,
                "NEW_MESSAGE",
                "Новое сообщение от @" + senderUsername,
                "В чате по заданию «" + taskTitle + "»",
                "/chat/" + roomId,
                Map.of("roomId", roomId, "fromUsername", senderUsername, "taskTitle", taskTitle)
        );
    }

    /** По реферальной ссылке зарегистрировался новый пользователь */
    public void notifyReferralRegistered(UUID inviterId, String newUsername) {
        send(
                inviterId,
                "XP_AWARDED",
                "Новый реферал!",
                "@" + newUsername + " зарегистрировался по вашей ссылке. Вы получили бонус",
                "/profile/" + inviterId,
                Map.of("fromUsername", newUsername)
        );
    }

    // ── Планировщик: чистка старых уведомлений ───────────────

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOlderThan(threshold);
        log.info("Старые уведомления (>30 дней) удалены");
    }

    // ── Mapper ────────────────────────────────────────────────

    private NotificationDtos.NotificationResponse toDto(NotificationEntity n) {
        NotificationDtos.NotificationResponse r = new NotificationDtos.NotificationResponse();
        r.setId(n.getId().toString());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setBody(n.getBody());
        r.setIsRead(n.getIsRead());
        r.setLink(n.getLink());
        r.setMeta(n.getMeta());
        r.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return r;
    }
}
