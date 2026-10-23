package com.mortgage.reconciliation.repository;

import com.mortgage.reconciliation.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    /**
     * Находит активный шаблон по типу документа вместе с полями.
     *
     * <p>Для одного {@code docType} база допускает не более одного активного шаблона
     * (уникальный частичный индекс {@code uq_template_active_doc_type}).
     *
     * @param docType тип документа, например {@code DDU}
     * @return активный шаблон с загруженными полями
     */
    @Query("""
            select distinct t
            from Template t
            left join fetch t.fields
            where t.docType = :docType
              and t.isActive = true
            """)
    Optional<Template> findActiveByDocType(@Param("docType") String docType);
}
