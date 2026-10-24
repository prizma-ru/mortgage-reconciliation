package com.mortgage.reconciliation.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Залог из массива {@code CreditApplication.Pledges}.
 *
 * @param id                   идентификатор записи (UUID)
 * @param estateType           тип недвижимости ({@code cm.RealEstateKind.V1})
 * @param objectCategory       категория объекта ({@code cm.RealEstateCategory.V1})
 * @param collateralKind       тип залога, вид обеспечения
 * @param verifiedValue        верифицированная стоимость
 * @param valueOfSaleDocument  стоимость приобретения по договору
 * @param area                 общая площадь, кв.м.
 * @param livingArea           жилая площадь, кв.м.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PledgesDto(
        @JsonProperty("Id") String id,
        @JsonProperty("EstateType") String estateType,
        @JsonProperty("ObjectCategory") String objectCategory,
        @JsonProperty("CollateralKind") String collateralKind,
        @JsonProperty("VerifiedValue") BigDecimal verifiedValue,
        @JsonProperty("ValueOfSaleDocument") BigDecimal valueOfSaleDocument,
        @JsonProperty("Area") BigDecimal area,
        @JsonProperty("LivingArea") BigDecimal livingArea
) {
}
