package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

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
 * @param location      откуда взято значение в документе
 * @param status        результат сравнения
 * @param message       пояснение для пользователя
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Строка протокола сверки: значение заявки против значения договора")
public record ComparisonResult(
        @Schema(description = "Код поля шаблона", example = "loan_amount") String fieldCode,
        @Schema(description = "Человекочитаемое имя поля", example = "Сумма кредита") String title,
        @Schema(description = "Нормализованное значение из заявки", example = "1000000.5")
        String appValue,
        @Schema(description = "Нормализованное значение из договора", example = "1000000.5")
        String contractValue,
        @Schema(description = "Где значение найдено в документе", example = "paragraph 1")
        String location,
        @Schema(description = "Результат сравнения", example = "OK") ComparisonStatus status,
        @Schema(description = "Пояснение для пользователя", example = "Значения совпадают")
        String message
) {
}
