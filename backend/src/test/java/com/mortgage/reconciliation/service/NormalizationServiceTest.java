package com.mortgage.reconciliation.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationServiceTest {

    private final NormalizationService service = new NormalizationService();

    @Test
    void normalizesMoneyWithCurrencyAndComma() {
        assertThat(service.normalizeMoney("1 000 000,50 руб.")).isEqualByComparingTo(new BigDecimal("1000000.50"));
        assertThat(service.normalizeMoney("1\u00A0000\u00A0000")).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(service.normalizeMoney("2 500 ₽")).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(service.normalizeMoney("1.000.000")).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void returnsNullForUnparseableMoney() {
        assertThat(service.normalizeMoney("без суммы")).isNull();
        assertThat(service.normalizeMoney("")).isNull();
        assertThat(service.normalizeMoney(null)).isNull();
    }

    @Test
    void normalizesPercent() {
        assertThat(service.normalizePercent("6,5 %")).isEqualByComparingTo(new BigDecimal("6.5"));
        assertThat(service.normalizePercent("12,5 % годовых")).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(service.normalizePercent("20%")).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void normalizesFio() {
        assertThat(service.normalizeFio("иванов иван")).isEqualTo("Иванов Иван");
        assertThat(service.normalizeFio("  иванов   иван   иванович ")).isEqualTo("Иванов Иван Иванович");
        assertThat(service.normalizeFio("ПЕТРОВА-ВОДКИНА АННА")).isEqualTo("Петрова-Водкина Анна");
        assertThat(service.normalizeFio("   ")).isNull();
    }

    @Test
    void parsesDateInBothFormats() {
        assertThat(service.normalizeDate("15.01.2025")).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(service.normalizeDate("15 января 2025")).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(service.normalizeDate("1 декабря 2024 г.")).isEqualTo(LocalDate.of(2024, 12, 1));
    }

    @Test
    void returnsNullForInvalidDate() {
        assertThat(service.normalizeDate("31.02.2025")).isNull();
        assertThat(service.normalizeDate("вчера")).isNull();
    }

    @Test
    void dispatchesByRule() {
        assertThat(service.normalize("1 000 000,50 руб.", "MONEY")).isEqualTo(new BigDecimal("1000000.50"));
        assertThat(service.normalize("6,5 %", "percent")).isEqualTo(new BigDecimal("6.5"));
        assertThat(service.normalize(" иванов  иван ", "FIO")).isEqualTo("Иванов Иван");
        assertThat(service.normalize("15.01.2025", "DATE")).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(service.normalize("  произвольный   текст ", null)).isEqualTo("произвольный текст");
        assertThat(service.normalize("   ", "MONEY")).isNull();
    }
}
