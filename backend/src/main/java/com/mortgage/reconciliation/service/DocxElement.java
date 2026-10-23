package com.mortgage.reconciliation.service;

/**
 * Единица содержимого .docx документа, сохраняющая исходный порядок блоков.
 */
public sealed interface DocxElement permits DocxElement.ParagraphBlock, DocxElement.TableCellBlock {

    /**
     * Параграф верхнего уровня.
     *
     * @param text текст параграфа
     */
    record ParagraphBlock(String text) implements DocxElement {
    }

    /**
     * Ячейка таблицы с её позицией.
     *
     * @param tableIndex порядковый номер таблицы в документе (с нуля)
     * @param rowIndex   индекс строки в таблице (с нуля)
     * @param colIndex   индекс ячейки в строке (с нуля)
     * @param text       текст ячейки
     */
    record TableCellBlock(int tableIndex, int rowIndex, int colIndex, String text) implements DocxElement {
    }
}
