package com.mortgage.reconciliation.dto;

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
public record FieldResultResponse(
        String fieldCode,
        String title,
        String appValue,
        String contractValue,
        String status,
        String message,
        String location
) {
}
