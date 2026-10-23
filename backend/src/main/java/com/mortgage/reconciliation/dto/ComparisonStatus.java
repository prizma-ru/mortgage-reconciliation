package com.mortgage.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат сравнения значения из заявки со значением из договора.
 */
@Schema(description = "Результат сравнения поля: OK, WARN, ERROR, MANUAL или MISSING")
public enum ComparisonStatus {

    /** Значения совпадают. */
    @Schema(description = "Значения после нормализации совпадают")
    OK,

    /** Поле есть в договоре, но отсутствует в заявке — нужно проверить вручную. */
    @Schema(description = "Поле есть в договоре, но отсутствует в заявке")
    WARN,

    /** Значения найдены, но различаются. */
    @Schema(description = "Значения найдены, но различаются")
    ERROR,

    /** Значение неоднозначно (несколько разных в договоре) или не нормализуется. */
    @Schema(description = "Несколько разных значений или значение не нормализуется")
    MANUAL,

    /** Поле не найдено в договоре. */
    @Schema(description = "Поле не найдено в договоре")
    MISSING
}
