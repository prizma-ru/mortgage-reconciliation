package com.mortgage.reconciliation.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Данные кредитной заявки АЗС — ключевые поля для сверки с договором.
 *
 * <p>Из документа взяты 20 наиболее значимых атрибутов: параметры кредита, каналы продаж,
 * участники сделки и залоги. Остальные атрибуты (их больше сотни) не участвуют в сверке
 * и игнорируются.
 *
 * @param type                       тип заявки ({@code cm.LoanApplicationType.V1})
 * @param division                   подразделение подачи заявки
 * @param divisionCity               город подразделения-инициатора
 * @param branch                     код филиала (БИСквит)
 * @param salesChannel               канал продажи
 * @param createChannel              канал создания
 * @param loanIssueChannel           канал выдачи кредита
 * @param loanProductGroup           код продуктовой группы
 * @param organizationUnitIdentification подразделение выдачи заявки
 * @param requestedLoanAmount        запрошенная сумма кредита
 * @param requestedLoanTerm          запрошенный срок кредита, мес
 * @param initialPayment             первоначальный взнос
 * @param pledgeRate                 ставка на залоговый период
 * @param loanCurrency               валюта кредита
 * @param liabilityType              тип кредитного обязательства
 * @param complexInsuranceFlag       наличие комплексного страхования
 * @param maternalCapitalAmount      сумма материнского капитала
 * @param houseRealEstateCost        стоимость строительства жилого дома
 * @param personalApplicants         участники сделки, кратность {@code [1..N]}
 * @param pledges                    залоги, кратность {@code [0..N]}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditApplicationDto(
        @JsonProperty("Type") String type,
        @JsonProperty("Division") String division,
        @JsonProperty("DivisionCity") String divisionCity,
        @JsonProperty("Branch") String branch,
        @JsonProperty("SalesChannel") String salesChannel,
        @JsonProperty("CreateChannel") String createChannel,
        @JsonProperty("LoanIssueChannel") String loanIssueChannel,
        @JsonProperty("LoanProductGroup") String loanProductGroup,
        @JsonProperty("OrganizationUnitIdentification") String organizationUnitIdentification,
        @JsonProperty("RequestedLoanAmount") BigDecimal requestedLoanAmount,
        @JsonProperty("RequestedLoanTerm") Integer requestedLoanTerm,
        @JsonProperty("InitialPayment") BigDecimal initialPayment,
        @JsonProperty("PledgeRate") BigDecimal pledgeRate,
        @JsonProperty("LoanCurrency") String loanCurrency,
        @JsonProperty("LiabilityType") String liabilityType,
        @JsonProperty("ComplexInsuranceFlag") Boolean complexInsuranceFlag,
        @JsonProperty("MaternalCapitalAmount") BigDecimal maternalCapitalAmount,
        @JsonProperty("HouseRealEstateCost") BigDecimal houseRealEstateCost,
        @JsonProperty("PersonalApplicants") List<PersonalApplicantsDto> personalApplicants,
        @JsonProperty("Pledges") List<PledgesDto> pledges
) {
}
