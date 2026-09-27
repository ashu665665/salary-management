package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The headline figures: what the organisation costs, what a typical person earns, and how fast pay
 * is moving.
 *
 * @param asOf                    the date the figures describe
 * @param baseCurrency            the currency every amount here has been converted to
 * @param averageIncreasePercent  average change against what people earned twelve months earlier
 */
public record PayrollOverview(
        LocalDate asOf,
        String baseCurrency,
        long headcount,
        BigDecimal totalAnnualCost,
        BigDecimal medianSalary,
        BigDecimal averageIncreasePercent) {
}
