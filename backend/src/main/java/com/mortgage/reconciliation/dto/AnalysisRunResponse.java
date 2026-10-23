package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisFieldResult;
import com.mortgage.reconciliation.domain.AnalysisRun;

import java.time.Instant;
import java.util.List;

/**
 * Запуск анализа вместе с результатами по полям — ответ REST API.
 *
 * @param runId     идентификатор запуска
 * @param docType   тип документа
 * @param fileName  имя загруженного файла
 * @param status    общий статус запуска ({@code DONE} / {@code NEEDS_REVIEW})
 * @param createdAt когда выполнен запуск
 * @param results   результаты по полям шаблона
 */
public record AnalysisRunResponse(
        Long runId,
        String docType,
        String fileName,
        String status,
        Instant createdAt,
        List<FieldResultResponse> results
) {

    /**
     * Преобразует сущность запуска в DTO.
     *
     * @param run запуск с загруженными результатами
     * @return DTO для фронтенда
     */
    public static AnalysisRunResponse from(AnalysisRun run) {
        List<FieldResultResponse> fields = run.getResults().stream()
                .map(AnalysisRunResponse::toFieldResult)
                .toList();

        return new AnalysisRunResponse(
                run.getId(),
                run.getDocType(),
                run.getFileName(),
                run.getStatus().name(),
                run.getCreatedAt(),
                fields
        );
    }

    private static FieldResultResponse toFieldResult(AnalysisFieldResult result) {
        return new FieldResultResponse(
                result.getFieldCode(),
                result.getTitle(),
                result.getAppValue(),
                result.getContractValue(),
                result.getStatus().name(),
                result.getMessage(),
                result.getLocation()
        );
    }
}
