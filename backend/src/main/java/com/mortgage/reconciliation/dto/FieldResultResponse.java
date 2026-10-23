package com.mortgage.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат сверки по одному полю в ответе REST API.
 *
 * @param fieldCode     код поля шаблона
 * @param title         человекочитаемое имя поля
 * @param appValue      значение из заявки
 * @param contractValue значение из договора
 * @param status        статус сравнения
 * @param message       пояснение
 * @param location      откуда взято значение в документе
 */
@Schema(description = "Результат сверки по одному полю")
public record FieldResultResponse(
        @Schema(description = "Код поля шаблона", example = "loan_amount") String fieldCode,
        @Schema(description = "Человекочитаемое имя поля", example = "Сумма кредита") String title,
        @Schema(description = "Значение из заявки", example = "1000000.5") String appValue,
        @Schema(description = "Значение из договора", example = "1000000.5") String contractValue,
        @Schema(description = "Статус сравнения", example = "OK") String status,
        @Schema(description = "Пояснение для пользователя", example = "Значения совпадают")
        String message,
        @Schema(description = "Где значение найдено в документе", example = "paragraph 1")
        String location
) {
}
