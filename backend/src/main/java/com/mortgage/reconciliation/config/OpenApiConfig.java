package com.mortgage.reconciliation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Метаданные OpenAPI для Swagger UI.
 *
 * <p>Схема эндпоинтов и DTO строится springdoc автоматически по аннотациям контроллеров
 * и {@code @Schema} на полях, поэтому здесь задаётся только общая информация о сервисе.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Описывает API в Swagger UI.
     *
     * @return описание сервиса (заголовок, назначение, версия)
     */
    @Bean
    public OpenAPI mortgageReconciliationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mortgage Reconciliation API")
                .description("Сервис автоматической сверки ипотечных документов")
                .version("1.0.0"));
    }
}
