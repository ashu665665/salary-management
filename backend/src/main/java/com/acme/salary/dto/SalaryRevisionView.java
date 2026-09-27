package com.acme.salary.dto;

import com.acme.salary.model.RevisionReason;
import com.acme.salary.model.SalaryRevision;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryRevisionView(
        Long id,
        MoneyView salary,
        LocalDate effectiveDate,
        RevisionReason reason,
        Instant recordedAt) {

    public static SalaryRevisionView of(SalaryRevision revision) {
        return new SalaryRevisionView(
                revision.getId(),
                MoneyView.of(revision.getSalary()),
                revision.getEffectiveDate(),
                revision.getReason(),
                revision.getRecordedAt());
    }
}
