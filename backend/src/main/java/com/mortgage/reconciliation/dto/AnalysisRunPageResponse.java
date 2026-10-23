package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisRun;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Страница истории запусков анализа.
 *
 * @param items         запуски на текущей странице
 * @param page          номер страницы (с нуля)
 * @param size          размер страницы
 * @param totalElements всего запусков
 * @param totalPages    всего страниц
 */
public record AnalysisRunPageResponse(
        List<AnalysisRunSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AnalysisRunPageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Возвращает неизменяемую копию списка запусков.
     *
     * @return запуски на странице
     */
    @Override
    public List<AnalysisRunSummaryResponse> items() {
        return List.copyOf(items);
    }

    /**
     * Преобразует страницу Spring Data в DTO.
     *
     * @param page страница запусков
     * @return страница для фронтенда
     */
    public static AnalysisRunPageResponse from(Page<AnalysisRun> page) {
        List<AnalysisRunSummaryResponse> items = page.getContent().stream()
                .map(AnalysisRunSummaryResponse::from)
                .toList();

        return new AnalysisRunPageResponse(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
