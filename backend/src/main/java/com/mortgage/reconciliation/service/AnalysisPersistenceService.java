package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.domain.AnalysisFieldResult;
import com.mortgage.reconciliation.domain.AnalysisRun;
import com.mortgage.reconciliation.domain.AnalysisRunStatus;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.ComparisonStatus;
import com.mortgage.reconciliation.repository.AnalysisRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Сохраняет результаты сверки в PostgreSQL и читает историю проверок.
 */
@Service
public class AnalysisPersistenceService {

    /** Максимальный размер страницы истории. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AnalysisRunRepository analysisRunRepository;

    /**
     * @param analysisRunRepository репозиторий запусков анализа
     */
    public AnalysisPersistenceService(AnalysisRunRepository analysisRunRepository) {
        this.analysisRunRepository = analysisRunRepository;
    }

    /**
     * Сохраняет запуск анализа вместе со всеми результатами по полям.
     *
     * <p>Общий статус: {@link AnalysisRunStatus#DONE}, если все поля в статусе
     * {@link ComparisonStatus#OK}, иначе {@link AnalysisRunStatus#NEEDS_REVIEW}.
     *
     * @param results  результаты сравнения
     * @param fileName имя загруженного файла
     * @param docType  тип документа
     * @return сохранённый запуск (его {@code id} — это {@code runId})
     */
    @Transactional
    public AnalysisRun save(List<ComparisonResult> results, String fileName, String docType) {
        List<ComparisonResult> safeResults = results == null ? List.of() : results;

        AnalysisRun run = new AnalysisRun(docType, fileName, overallStatus(safeResults));

        for (ComparisonResult result : safeResults) {
            if (result != null) {
                run.addResult(new AnalysisFieldResult(result));
            }
        }

        return analysisRunRepository.save(run);
    }

    /**
     * Читает запуск вместе с результатами по полям.
     *
     * @param runId идентификатор запуска
     * @return запуск с инициализированным списком результатов
     * @throws java.util.NoSuchElementException если запуск не найден
     */
    @Transactional(readOnly = true)
    public AnalysisRun findRun(Long runId) {
        Objects.requireNonNull(runId, "runId must not be null");

        return analysisRunRepository.findByIdWithResults(runId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Analysis run not found: " + runId));
    }

    /**
     * Возвращает страницу истории запусков (сначала новые).
     *
     * @param page номер страницы с нуля
     * @param size размер страницы
     * @return страница запусков без вложенных результатов
     */
    @Transactional(readOnly = true)
    public Page<AnalysisRun> findRuns(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return analysisRunRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(safePage, safeSize));
    }

    private AnalysisRunStatus overallStatus(List<ComparisonResult> results) {
        // DONE ставим только когда все поля совпали: любой WARN/ERROR/MANUAL/MISSING
        // означает, что сотрудник должен открыть протокол.
        boolean allOk = results.stream()
                .filter(Objects::nonNull)
                .allMatch(result -> result.status() == ComparisonStatus.OK);

        return allOk ? AnalysisRunStatus.DONE : AnalysisRunStatus.NEEDS_REVIEW;
    }
}
