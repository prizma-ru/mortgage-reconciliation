package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisRun;

import java.time.Instant;

/**
 * Краткая информация о запуске анализа для списка истории.
 *
 * @param runId     идентификатор запуска
 * @param docType   тип документа
 * @param fileName  имя загруженного файла
 * @param status    общий статус запуска
 * @param createdAt когда выполнен запуск
 */
public record AnalysisRunSummaryResponse(
        Long runId,
        String docType,
        String fileName,
        String status,
        Instant createdAt
) {

    /**
     * Преобразует сущность запуска в краткое DTO.
     *
     * @param run запуск анализа
     * @return краткая информация для списка
     */
    public static AnalysisRunSummaryResponse from(AnalysisRun run) {
        return new AnalysisRunSummaryResponse(
                run.getId(),
                run.getDocType(),
                run.getFileName(),
                run.getStatus().name(),
                run.getCreatedAt()
        );
    }
}
