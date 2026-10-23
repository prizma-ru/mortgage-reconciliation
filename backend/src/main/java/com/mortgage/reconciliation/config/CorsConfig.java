package com.mortgage.reconciliation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS для локального фронтенда.
 *
 * <p>Разрешены только два адреса Vite-дев-сервера: браузер обращается к API напрямую,
 * когда прокси не используется. Список намеренно не расширяется до {@code *}, чтобы
 * случайно не открыть API произвольному origin.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Регистрирует правила CORS для всех путей API.
     *
     * @param registry реестр CORS-правил Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // включая /actuator/health
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
