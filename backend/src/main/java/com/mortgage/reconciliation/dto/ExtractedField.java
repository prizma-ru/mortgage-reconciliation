package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Извлечённое значение поля договора до нормализации")
public record ExtractedField(
        @Schema(description = "Код поля шаблона", example = "loan_amount") String fieldCode,
        @Schema(description = "Найденный фрагмент текста без нормализации", example = "1 000 000,50")
        String rawValue,
        @Schema(description = "Откуда извлечено: параграф или координаты ячейки таблицы",
                example = "paragraph 1")
        String location,
        @Schema(description = "Уверенность от 0 до 1: выше, если сработала якорная фраза",
                example = "0.9")
        double confidence
) {
}
