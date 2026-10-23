package com.mortgage.reconciliation.repository;

import com.mortgage.reconciliation.domain.AnalysisRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

    /**
     * Загружает запуск вместе с результатами по полям одним запросом,
     * чтобы не получить {@code LazyInitializationException} при маппинге в DTO.
     *
     * @param id идентификатор запуска
     * @return запуск с инициализированным списком результатов
     */
    @Query("""
            select distinct r
            from AnalysisRun r
            left join fetch r.results
            where r.id = :id
            """)
    Optional<AnalysisRun> findByIdWithResults(@Param("id") Long id);

    /**
     * История запусков: сначала новые.
     *
     * @param pageable параметры страницы
     * @return страница запусков без загрузки результатов по полям
     */
    Page<AnalysisRun> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
