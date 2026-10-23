package com.mortgage.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public record DocBlock(
        BlockType type,
        String text,
        Integer tableIndex,
        Integer rowIndex,
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
