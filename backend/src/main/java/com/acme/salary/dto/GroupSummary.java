package com.acme.salary.dto;

import java.math.BigDecimal;

/**
 * One row of a breakdown: a country, department or level, and what it costs.
 *
 * <p>Quartiles as well as the median, because the spread within a group is usually the interesting
 * part: two groups with the same median can pay very differently.
 */
public record GroupSummary(
        String group,
        long headcount,
        BigDecimal totalAnnualCost,
        BigDecimal medianSalary,
        BigDecimal lowerQuartile,
        BigDecimal upperQuartile) {
}
