package com.mortgage.reconciliation.domain;

/**
 * Общий статус запуска анализа.
 */
public enum AnalysisRunStatus {

    /** Все поля сверены без замечаний. */
    DONE,

    /** Есть поля, требующие внимания сотрудника. */
    NEEDS_REVIEW
}
