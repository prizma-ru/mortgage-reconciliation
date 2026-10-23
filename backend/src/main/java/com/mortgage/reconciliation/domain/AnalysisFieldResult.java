package com.mortgage.reconciliation.domain;

import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.ComparisonStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Сохранённый результат сверки по одному полю шаблона.
 */
@Entity
@Table(name = "analysis_field_result")
public class AnalysisFieldResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AnalysisRun run;

    @Column(name = "field_code", nullable = false, length = 64)
    private String fieldCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "app_value", columnDefinition = "text")
    private String appValue;

    @Column(name = "contract_value", columnDefinition = "text")
    private String contractValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ComparisonStatus status;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "location", length = 255)
    private String location;

    protected AnalysisFieldResult() {
    }

    public AnalysisFieldResult(ComparisonResult source) {
        this.fieldCode = source.fieldCode();
        this.title = source.title();
        this.appValue = source.appValue();
        this.contractValue = source.contractValue();
        this.status = source.status();
        this.message = source.message();
        this.location = source.location();
    }

    public Long getId() {
        return id;
    }

    public AnalysisRun getRun() {
        return run;
    }

    public void setRun(AnalysisRun run) {
        this.run = run;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getTitle() {
        return title;
    }

    public String getAppValue() {
        return appValue;
    }

    public String getContractValue() {
        return contractValue;
    }

    public ComparisonStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getLocation() {
        return location;
    }
}
