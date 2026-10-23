package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisFieldResult;
import com.mortgage.reconciliation.domain.AnalysisRun;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Сохранённый запуск сверки вместе с результатами по полям")
public record AnalysisRunResponse(
        @Schema(description = "Идентификатор запуска", example = "12") Long runId,
        @Schema(description = "Тип документа", example = "DDU") String docType,
        @Schema(description = "Имя загруженного файла", example = "contract.docx") String fileName,
        @Schema(description = "Общий статус запуска", example = "NEEDS_REVIEW") String status,
        @Schema(description = "Когда выполнен запуск", example = "2026-10-23T20:09:48Z")
        Instant createdAt,
        @Schema(description = "Результаты по полям шаблона") List<FieldResultResponse> results
) {

    public AnalysisRunResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }

    /**
     * Возвращает неизменяемую копию списка результатов.
     *
     * @return результаты по полям
     */
    @Override
    public List<FieldResultResponse> results() {
        return List.copyOf(results);
    }

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
