package com.mortgage.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа сервиса сверки ипотечных документов.
 *
 * <p>Приложение стартует без бизнес-инициализации: схема БД приводится в актуальное состояние
 * Flyway-миграциями, шаблоны извлечения читаются из таблиц при первом запросе, поэтому
 * дополнительный код запуска не нужен.
 */
@SpringBootApplication
public class MortgageReconciliationApplication {

    /**
     * Запускает Spring Boot приложение.
     *
     * @param args аргументы командной строки, например {@code --spring.profiles.active=dev}
     */
    public static void main(String[] args) {
        SpringApplication.run(MortgageReconciliationApplication.class, args);
    }
}
