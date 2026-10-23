package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Одна строка результата сверки: значение из заявки против значения из договора.
 *
 * <p>{@code appValue} и {@code contractValue} содержат нормализованные значения
 * (или исходные, если нормализовать не удалось).
 *
 * @param fieldCode     код поля шаблона
 * @param title         человекочитаемое имя поля
 * @param appValue      значение из заявки
 * @param contractValue значение из договора
 * @param status        результат сравнения
 * @param message       пояснение для пользователя
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComparisonResult(
        String fieldCode,
        String title,
        String appValue,
        String contractValue,
        ComparisonStatus status,
        String message
) {
}
