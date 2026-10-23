package com.mortgage.reconciliation.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Шаблон документа: набор правил извлечения для конкретного типа документа.
 */
@Entity
@Table(name = "template")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplateField> fields = new ArrayList<>();

    protected Template() {
    }

    public Template(String docType, String name) {
        this.docType = docType;
        this.name = name;
        this.isActive = true;
    }

    /**
     * Добавляет поле шаблона и проставляет обратную ссылку.
     *
     * @param field поле шаблона
     */
    public void addField(TemplateField field) {
        field.setTemplate(this);
        fields.add(field);
    }

    public Long getId() {
        return id;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Возвращает неизменяемую копию полей: коллекция управляется только сущностью.
     *
     * @return поля шаблона
     */
    public List<TemplateField> getFields() {
        return List.copyOf(fields);
    }
}
