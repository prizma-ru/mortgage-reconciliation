package com.mortgage.reconciliation.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Участник сделки из массива {@code CreditApplication.PersonalApplicants}.
 *
 * @param id                 идентификатор клиента (UUID)
 * @param role               роль в заявке ({@code cm.CustomerLoanApplicationRole.V1})
 * @param clientType         тип клиента (ФЛ, ЮЛ, ИП)
 * @param surname            фамилия
 * @param name               имя
 * @param patronymic         отчество
 * @param birthDay           дата рождения в формате {@code yyyy-mm-dd}
 * @param inn                ИНН клиента
 * @param sex                пол ({@code cm.Gender.V1})
 * @param citizenCountryCode код страны гражданства
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonalApplicantsDto(
        @JsonProperty("Id") String id,
        @JsonProperty("Role") String role,
        @JsonProperty("ClientType") String clientType,
        @JsonProperty("Surname") String surname,
        @JsonProperty("Name") String name,
        @JsonProperty("Patronymic") String patronymic,
        @JsonProperty("BirthDay") LocalDate birthDay,
        @JsonProperty("INN") String inn,
        @JsonProperty("Sex") String sex,
        @JsonProperty("CitizenCountryCode") String citizenCountryCode
) {
}
