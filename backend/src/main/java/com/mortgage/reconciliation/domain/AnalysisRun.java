package com.mortgage.reconciliation.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Запуск сверки конкретного документа с заявкой.
 */
@Entity
@Table(name = "analysis_run")
public class AnalysisRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisRunStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AnalysisFieldResult> results = new ArrayList<>();

    protected AnalysisRun() {
    }

    public AnalysisRun(String docType, String fileName, AnalysisRunStatus status) {
        this.docType = docType;
        this.fileName = fileName;
        this.status = status;
        this.createdAt = Instant.now();
    }

    /**
     * Добавляет результат по полю и проставляет обратную ссылку.
     *
     * @param result результат сравнения поля
     */
    public void addResult(AnalysisFieldResult result) {
        result.setRun(this);
        results.add(result);
    }

    public Long getId() {
        return id;
    }

    public String getDocType() {
        return docType;
    }

    public String getFileName() {
        return fileName;
    }

    public AnalysisRunStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisRunStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Возвращает неизменяемую копию результатов: коллекция управляется только сущностью.
     *
     * @return результаты по полям
     */
    public List<AnalysisFieldResult> getResults() {
        return List.copyOf(results);
    }
}
