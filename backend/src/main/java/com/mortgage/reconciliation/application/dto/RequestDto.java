package com.mortgage.reconciliation.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Запрос заявки: идентификатор, версия и данные кредитной заявки.
 *
 * @param creditApplicationId идентификатор заявки ({@code CreditApplicationId})
 * @param version             версия заявки, кратность {@code [0..1]}
 * @param creditApplication   данные о заявке
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestDto(
        @JsonProperty("CreditApplicationId") String creditApplicationId,
        @JsonProperty("Version") Integer version,
        @JsonProperty("CreditApplication") CreditApplicationDto creditApplication
) {
}
