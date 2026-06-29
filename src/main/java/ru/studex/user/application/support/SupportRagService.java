package ru.studex.user.application.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RAG-оркестратор с in-memory хранением эмбеддингов.
 * Статьи захардкожены, эмбеддинги вычисляются один раз при старте через Ollama.
 * Если Ollama недоступна — отвечает без контекста (только LLM).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportRagService {

    private final OllamaEmbeddingService embeddingService;
    private final GroqChatService groqChatService;

    private final AtomicReference<List<ArticleEmbedding>> cache = new AtomicReference<>(List.of());

    private record ArticleEmbedding(String content, String category, float[] embedding) {}

    public record ChatMessage(String role, String content) {}

    public record AnswerResult(String reply, boolean escalated) {}

    private static final List<String> ESCALATION_TRIGGERS = List.of(
        "оператор", "сотрудник", "живой человек", "реальный человек",
        "человека", "с человеком", "свяжите", "соедините", "позвоните",
        "не хочу с ботом", "не хочу с ai", "хочу менеджера", "ваш менеджер"
    );

    // ── Knowledge base ────────────────────────────────────────────────────────

    private static final List<String[]> ARTICLES = List.of(
        // {category, content}
        new String[]{"tasks", "Как создать задание: нажмите кнопку «Новое задание» в верхнем меню или боковой панели. Заполните название, описание, бюджет и дедлайн. Задание сразу появится в маркетплейсе."},
        new String[]{"tasks", "Как выбрать исполнителя: после публикации задания дождитесь откликов. Откройте карточку задания, перейдите во вкладку «Отклики» и нажмите «Выбрать» рядом с подходящим кандидатом."},
        new String[]{"tasks", "Что делать если исполнитель не сдал работу в срок: нажмите «Открыть спор» в карточке задания. Модератор рассмотрит ситуацию в течение 24 часов (PRO — 2 часа). Возможные исходы: возврат средств, продление срока или частичная оплата."},
        new String[]{"tasks", "Как отредактировать задание: откройте задание в разделе «Мои задания», нажмите три точки → «Редактировать». Изменения вступят в силу немедленно, исполнители получат уведомление."},
        new String[]{"tasks", "Как повысить видимость задания: используйте функцию Boost (продвижение). Продвинутые задания показываются выше в поиске и получают больше откликов."},
        new String[]{"payments", "Как работает Escrow-оплата: при выборе исполнителя средства замораживаются на платформе. Они поступят исполнителю только после того, как заказчик нажмёт «Принять работу». Это защищает обе стороны."},
        new String[]{"payments", "Когда выводятся деньги: вывод доступен после завершения заказа. Обработка занимает 1–3 рабочих дня в зависимости от способа вывода."},
        new String[]{"payments", "Какие комиссии: стандартная комиссия составляет 10% от суммы заказа. Для PRO-пользователей — 7%. При выводе взимается дополнительная комиссия платёжного провайдера."},
        new String[]{"payments", "Как пополнить баланс: перейдите в раздел «Финансы», выберите способ пополнения — карта, СБП. Пополнение обычно мгновенно."},
        new String[]{"account", "Как повысить рейтинг исполнителя: рейтинг формируется из оценок заказчиков, скорости ответа, процента выполненных заказов и активности. Выполняйте задания в срок и общайтесь вежливо."},
        new String[]{"account", "Как зарегистрироваться: регистрация бесплатная. Выберите роль (студент или исполнитель), заполните форму с именем, email и паролём. Подтвердите email — готово."},
        new String[]{"account", "Смена роли: переключиться со студента на исполнителя можно в настройках профиля. Обратная смена — только через службу поддержки (создайте тикет)."},
        new String[]{"account", "Верификация исполнителя: пройдите верификацию в профиле для значка «Проверено». Верифицированные исполнители получают больше доверия и выше ранжируются в поиске."},
        new String[]{"subscription", "Чем отличается PRO от BASE: PRO включает безлимитные отклики, приоритетную поддержку (5 мин), сниженную комиссию 7%, личного менеджера и ранний доступ к новым функциям."},
        new String[]{"subscription", "Как отменить подписку: Настройки → Подписка → «Отменить». Подписка останется активной до конца периода. Деньги за неиспользованные дни не возвращаются."},
        new String[]{"subscription", "Приоритетная поддержка PRO: PRO-пользователи попадают в отдельную очередь. Среднее время ответа оператора — 5 минут вместо 30 минут."},
        new String[]{"subscription", "LITE план бесплатный: базовый LITE всегда бесплатен. Включает ограниченное количество откликов и стандартную поддержку."},
        new String[]{"general", "Как связаться с поддержкой: напишите в AI-чат, создайте тикет в разделе «Поддержка → Тикет». PRO-пользователи получают ответ в течение 5 минут."},
        new String[]{"general", "Что такое StuForEx: маркетплейс учебных заданий, где студенты находят исполнителей для курсовых, рефератов, задач по программированию, дизайна и других заданий."},
        new String[]{"general", "Как открыть спор: нажмите кнопку «Открыть спор» в карточке задания если исполнитель не выполнил работу или нарушил условия. Модератор подключится в течение 24 часов."}
    );

    private static final String SYSTEM_PROMPT = """
            Ты AI-ассистент платформы StuForEx — маркетплейс учебных заданий для студентов.

            Правила:
            1. Отвечай ТОЛЬКО на основе предоставленного контекста из базы знаний.
            2. Если в контексте нет ответа — честно скажи и предложи создать тикет.
            3. Отвечай кратко, по делу, дружелюбно, на русском языке.
            4. Не выдумывай информацию, которой нет в контексте.

            Основные факты:
            - Escrow-оплата: деньги замораживаются при выборе исполнителя
            - Комиссия: 10% (PRO — 7%)
            - Споры решаются за 24 ч (PRO — 2 ч)
            - PRO: безлимит откликов, поддержка 5 мин, комиссия 7%
            """;

    // ── Async initialization ──────────────────────────────────────────────────

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void buildEmbeddingsCache() {
        log.info("Building in-memory embeddings for {} knowledge articles...", ARTICLES.size());
        List<ArticleEmbedding> result = new ArrayList<>();
        for (String[] article : ARTICLES) {
            try {
                float[] emb = embeddingService.embed(article[1]);
                result.add(new ArticleEmbedding(article[1], article[0], emb));
                Thread.sleep(150);
            } catch (Exception e) {
                log.debug("Skipping article embedding (Ollama likely not running): {}", e.getMessage());
            }
        }
        cache.set(result);
        if (result.isEmpty()) {
            log.warn("No embeddings cached — RAG disabled. Install Ollama and run: ollama pull nomic-embed-text");
        } else {
            log.info("Embeddings ready: {}/{} articles indexed in memory.", result.size(), ARTICLES.size());
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    public AnswerResult answer(String userMessage, List<ChatMessage> history, boolean isPro) {
        String lower = userMessage.toLowerCase();
        boolean escalate = ESCALATION_TRIGGERS.stream().anyMatch(lower::contains);

        // Also escalate if bot failed to help in last 3 turns
        if (!escalate && history.size() >= 6) {
            long failCount = history.stream()
                    .filter(m -> "assistant".equals(m.role()))
                    .filter(m -> m.content().contains("не могу") || m.content().contains("создайте тикет"))
                    .count();
            if (failCount >= 2) escalate = true;
        }

        String context = findContext(userMessage);
        String systemPrompt = buildSystemPrompt(context, isPro, escalate);
        List<Map<String, String>> messages = buildMessages(systemPrompt, history, userMessage);
        String reply = groqChatService.chat(messages);
        return new AnswerResult(reply, escalate);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String findContext(String query) {
        List<ArticleEmbedding> articles = cache.get();
        if (articles.isEmpty()) return "";

        float[] queryEmb;
        try {
            queryEmb = embeddingService.embed(query);
        } catch (Exception e) {
            log.debug("Embed query failed: {}", e.getMessage());
            return "";
        }

        return articles.stream()
                .map(a -> Map.entry(a, cosineSimilarity(queryEmb, a.embedding())))
                .filter(e -> e.getValue() > 0.55)
                .sorted(Map.Entry.<ArticleEmbedding, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> "[" + e.getKey().category() + "] " + e.getKey().content())
                .reduce("", (a, b) -> a + "\n\n" + b)
                .trim();
    }

    private String buildSystemPrompt(String context, boolean isPro, boolean escalating) {
        String base = SYSTEM_PROMPT;
        if (!context.isBlank()) {
            base += "\n\n=== Контекст из базы знаний ===\n" + context;
        }
        if (isPro) {
            base += "\n\n(Пользователь имеет PRO-подписку — упомяни приоритетные преимущества при уместности.)";
        }
        if (escalating) {
            base += "\n\nПользователь хочет поговорить с живым оператором. Ответь вежливо, что подключаешь оператора, и сообщи примерное время ожидания (" + (isPro ? "5 минут" : "30 минут") + ").";
        }
        return base;
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        int start = Math.max(0, history.size() - 8);
        history.subList(start, history.size())
               .forEach(m -> messages.add(Map.of("role", m.role(), "content", m.content())));
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }
}
