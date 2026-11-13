package it.somnia.dreamscape.api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Attiva @Async: i metodi annotati girano nel pool di thread che Spring Boot crea da solo
@Configuration
@EnableAsync
public class AsyncConfig {
}
