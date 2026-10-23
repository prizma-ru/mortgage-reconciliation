package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Блок документа: параграф или ячейка таблицы.
 *
 * <p>Для параграфа заполнены только {@code type} и {@code text},
 * для ячейки дополнительно заполняются координаты {@code tableIndex},
 * {@code rowIndex} и {@code colIndex}. Неиспользуемые поля в JSON не попадают.
 *
 * @param type       тип блока
 * @param text       нормализованный текст блока
 * @param tableIndex порядковый номер таблицы в документе (с нуля), только для {@link BlockType#TABLE_CELL}
 * @param rowIndex   индекс строки таблицы (с нуля), только для {@link BlockType#TABLE_CELL}
 * @param colIndex   индекс ячейки в строке (с нуля), только для {@link BlockType#TABLE_CELL}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Блок документа: параграф или ячейка таблицы")
public record DocBlock(
        @Schema(description = "Тип блока", example = "PARAGRAPH") BlockType type,
        @Schema(description = "Нормализованный текст блока", example = "Сумма кредита: 1 000 000 руб.")
        String text,
        @Schema(description = "Номер таблицы в документе; только для TABLE_CELL", example = "0")
        Integer tableIndex,
        @Schema(description = "Индекс строки таблицы; только для TABLE_CELL", example = "0")
        Integer rowIndex,
        @Schema(description = "Индекс ячейки в строке; только для TABLE_CELL", example = "1")
        Integer colIndex
) {

    /**
     * Создаёт блок параграфа.
     *
     * @param text нормализованный текст
     * @return блок с типом {@link BlockType#PARAGRAPH} и без координат
     */
    public static DocBlock paragraph(String text) {
        return new DocBlock(BlockType.PARAGRAPH, text, null, null, null);
    }

    /**
     * Создаёт блок ячейки таблицы.
     *
     * @param tableIndex номер таблицы в документе (с нуля)
     * @param rowIndex   индекс строки (с нуля)
     * @param colIndex   индекс ячейки (с нуля)
     * @param text       нормализованный текст ячейки
     * @return блок с типом {@link BlockType#TABLE_CELL} и координатами
     */
    public static DocBlock tableCell(int tableIndex, int rowIndex, int colIndex, String text) {
        return new DocBlock(BlockType.TABLE_CELL, text, tableIndex, rowIndex, colIndex);
    }
}
