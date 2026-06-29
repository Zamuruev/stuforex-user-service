package ru.studex.user.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Использует SimpleAsyncTaskExecutor по умолчанию.
    // При необходимости замени на ThreadPoolTaskExecutor с нужными параметрами.
}
