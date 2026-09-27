package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import java.math.BigDecimal;

/**
 * Someone paid outside the range of their peers, with the range they were compared against so the
 * HR Manager can judge whether it is a problem or a deliberate decision.
 *
 * @param position BELOW_RANGE or ABOVE_RANGE
 */
public record PayOutlier(
        Long employeeId,
        String employeeCode,
        String fullName,
        Country country,
        Department department,
        JobLevel jobLevel,
        BigDecimal salary,
        BigDecimal peerMedian,
        BigDecimal lowerQuartile,
        BigDecimal upperQuartile,
        String position) {
}
