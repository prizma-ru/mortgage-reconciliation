package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Результат извлечения одного значения по полю шаблона.
 *
 * <p>Если в документе встречаются разные значения одного поля, в ответе будет
 * несколько записей с одинаковым {@code fieldCode} — такие поля требуют ручной проверки.
 *
 * @param fieldCode  код поля, например {@code loan_amount}
 * @param rawValue   найденный фрагмент текста без нормализации
 * @param location   откуда извлечено: номер параграфа или координаты ячейки таблицы
 * @param confidence уверенность от 0 до 1: выше, если сработал якорь
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractedField(
        String fieldCode,
        String rawValue,
        String location,
        double confidence
) {
}
