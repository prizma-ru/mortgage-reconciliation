package com.mortgage.reconciliation.repository;

import com.mortgage.reconciliation.domain.AnalysisFieldResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisFieldResultRepository extends JpaRepository<AnalysisFieldResult, Long> {

    /**
     * Возвращает результаты по полям для указанного запуска.
     *
     * @param runId идентификатор запуска
     * @return результаты в порядке сохранения
     */
    List<AnalysisFieldResult> findByRunIdOrderByIdAsc(Long runId);
}
