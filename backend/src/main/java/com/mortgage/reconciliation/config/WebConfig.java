package com.mortgage.reconciliation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Регистрирует сквозной интерсептор заголовков.
 *
 * <p>Интерсептор применён ко всем путям: контекст нужен и API, и actuator-проверкам,
 * а лишние заголовки он не требует — при отсутствии генерирует {@code x-call-id}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final HeaderInterceptor headerInterceptor;

    /**
     * @param headerInterceptor интерсептор, заполняющий {@code HeaderContext}
     */
    public WebConfig(HeaderInterceptor headerInterceptor) {
        this.headerInterceptor = headerInterceptor;
    }

    /**
     * Добавляет интерсептор заголовков.
     *
     * @param registry реестр интерсепторов Spring MVC
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptor);
    }
}
