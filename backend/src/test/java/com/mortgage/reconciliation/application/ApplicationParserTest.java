package com.mortgage.reconciliation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mortgage.reconciliation.application.dto.MortgageApplicationDto;
import com.mortgage.reconciliation.exception.ApplicationParsingException;
import com.mortgage.reconciliation.service.ApplicationFlattenerService;
import com.mortgage.reconciliation.service.ApplicationParserService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationParserTest {

    /** JSON по образцу документа «Список атрибутов заявки на ипотеку». */
    private static final String APPLICATION_JSON = """
            {
              "SystemID": "ASZ",
              "RequestID": "0f1e2d3c-4b5a-6978-8a9b-0c1d2e3f4a5b",
              "UserID": "ivanov",
              "Request": {
                "CreditApplicationId": "1234567890123456789",
                "Version": 3,
                "CreditApplication": {
                  "Type": "cm.LoanApplicationType.V1",
                  "CreateChannel": "cm.ChannelKind.SalePoint",
                  "RequestedLoanAmount": 5000000.00,
                  "RequestedLoanTerm": 240,
                  "InitialPayment": 1000000.00,
                  "PledgeRate": 0.12,
                  "PersonalApplicants": [
                    {
                      "Role": "cm.CustomerLoanApplicationRole.Borrower",
                      "ClientType": "cm.CustomerType.Individual",
                      "Surname": "Иванов",
                      "Name": "Иван",
                      "Patronymic": "Иванович",
                      "BirthDay": "1990-05-15",
                      "INN": "770123456789"
                    }
                  ],
                  "Pledges": [
                    {
                      "ObjectCategory": "cm.RealEstateCategory.Readyhousing",
                      "Area": 54.5,
                      "ValueOfSaleDocument": 6000000.00
                    }
                  ]
                }
              }
            }
            """;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final ApplicationFlattenerService flattener = new ApplicationFlattenerService(objectMapper);
    private final ApplicationParserService parser = new ApplicationParserService(objectMapper, flattener);

    @Test
    void parsesNestedApplicationStructure() {
        MortgageApplicationDto application = parser.parse(APPLICATION_JSON);

        assertThat(application.systemId()).isEqualTo("ASZ");
        assertThat(application.request().creditApplicationId()).isEqualTo("1234567890123456789");
        assertThat(application.request().version()).isEqualTo(3);

        var credit = application.request().creditApplication();
        assertThat(credit.requestedLoanAmount()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(credit.requestedLoanTerm()).isEqualTo(240);
        assertThat(credit.pledgeRate()).isEqualByComparingTo(new BigDecimal("0.12"));

        assertThat(credit.personalApplicants()).hasSize(1);
        assertThat(credit.personalApplicants().get(0).surname()).isEqualTo("Иванов");
        assertThat(credit.personalApplicants().get(0).birthDay()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(credit.personalApplicants().get(0).inn()).isEqualTo("770123456789");

        assertThat(credit.pledges()).hasSize(1);
        assertThat(credit.pledges().get(0).objectCategory())
                .isEqualTo("cm.RealEstateCategory.Readyhousing");
        assertThat(credit.pledges().get(0).valueOfSaleDocument())
                .isEqualByComparingTo(new BigDecimal("6000000.00"));
    }

    @Test
    void flattensNestedApplicationWithDottedPathsAndIndexes() {
        Map<String, String> flat = parser.parseToFlatMap(APPLICATION_JSON);

        assertThat(flat).containsEntry("SystemID", "ASZ");
        assertThat(new BigDecimal(flat.get("Request.CreditApplication.RequestedLoanAmount")))
                .isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(flat).containsEntry("Request.CreditApplication.RequestedLoanTerm", "240");
        assertThat(flat).containsEntry("Request.CreditApplication.PersonalApplicants[0].Surname", "Иванов");
        assertThat(flat)
                .containsEntry("Request.CreditApplication.PersonalApplicants[0].BirthDay", "1990-05-15");
        assertThat(flat).containsEntry("Request.CreditApplication.Pledges[0].ObjectCategory",
                "cm.RealEstateCategory.Readyhousing");
        assertThat(new BigDecimal(flat.get("Request.CreditApplication.Pledges[0].ValueOfSaleDocument")))
                .isEqualByComparingTo(new BigDecimal("6000000"));

        assertThat(new BigDecimal(flat.get("Request.CreditApplication.PersonalApplicants[0].INN")))
                .isEqualByComparingTo(new BigDecimal("770123456789"));

        // Пути заканчиваются ожидаемыми сегментами из документа.
        assertThat(flat.keySet())
                .anyMatch(key -> key.endsWith("PersonalApplicants[0].Surname"))
                .anyMatch(key -> key.equals("Request.CreditApplication.RequestedLoanAmount"));
    }

    @Test
    void skipsNullFieldsWhenFlattening() {
        Map<String, String> flat = parser.parseToFlatMap("""
                {"Request": {"CreditApplication": {"RequestedLoanAmount": 100}}}
                """);

        assertThat(flat).containsOnlyKeys("Request.CreditApplication.RequestedLoanAmount");
        assertThat(flat).doesNotContainKey("SystemID");
    }

    @Test
    void keepsLegacyFlatApplicationFormat() {
        Map<String, String> flat = parser.parseToFlatMap("""
                {"loan_amount": "1000000", "interest_rate": "12.5"}
                """);

        assertThat(flat).containsEntry("loan_amount", "1000000");
        assertThat(flat).containsEntry("interest_rate", "12.5");
    }

    @Test
    void rejectsBrokenJsonWithReadableMessage() {
        assertThatThrownBy(() -> parser.parse("{\"SystemID\": "))
                .isInstanceOf(ApplicationParsingException.class)
                .hasMessageContaining("корректным JSON");
    }

    @Test
    void rejectsBlankApplication() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(ApplicationParsingException.class)
                .hasMessageContaining("пустая");
    }

    @Test
    void rejectsNonObjectJson() {
        assertThatThrownBy(() -> parser.parse("[1, 2, 3]"))
                .isInstanceOf(ApplicationParsingException.class)
                .hasMessageContaining("JSON-объектом");
    }
}
