package com.mortgage.reconciliation.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Корень заявки АЗС: идентификаторы внешней системы и вложенный запрос.
 *
 * <p>Документ описывает глубоко вложенную структуру, поэтому в DTO включены только блоки,
 * нужные для сверки. Неизвестные поля (их больше сотни) игнорируются Jackson'ом.
 *
 * @param systemId  код внешней системы источника ({@code SystemID})
 * @param requestId UUID внешней транзакции ({@code RequestID})
 * @param userId    логин сотрудника фронтальной системы ({@code UserID})
 * @param request   вложенный запрос с данными заявки
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MortgageApplicationDto(
        @JsonProperty("SystemID") String systemId,
        @JsonProperty("RequestID") String requestId,
        @JsonProperty("UserID") String userId,
        @JsonProperty("Request") RequestDto request
) {
}
