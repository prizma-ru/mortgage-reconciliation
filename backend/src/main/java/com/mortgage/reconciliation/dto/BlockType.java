package com.mortgage.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Тип извлечённого блока документа.
 */
@Schema(description = "Тип блока документа")
public enum BlockType {

    /** Параграф документа. */
    PARAGRAPH,

    /** Ячейка таблицы. */
    TABLE_CELL
}
