package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisRun;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Краткая информация о запуске анализа для списка истории")
public record AnalysisRunSummaryResponse(
        @Schema(description = "Идентификатор запуска", example = "12") Long runId,
        @Schema(description = "Тип документа", example = "DDU") String docType,
        @Schema(description = "Имя загруженного файла", example = "contract.docx") String fileName,
        @Schema(description = "Общий статус запуска", example = "DONE") String status,
        @Schema(description = "Когда выполнен запуск", example = "2026-10-23T20:09:48Z")
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
