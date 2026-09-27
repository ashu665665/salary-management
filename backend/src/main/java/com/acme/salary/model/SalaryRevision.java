package com.acme.salary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One salary change: what the employee earns from {@code effectiveDate} onwards, and why.
 *
 * <p>A revision cannot be edited once recorded. Fixing a mistake means recording a new revision,
 * which is the whole point of keeping history rather than a single editable figure. There are
 * getters but deliberately no setters.
 */
@Entity
@Table(name = "salary_revision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalaryRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Embedded
    private Money salary;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private RevisionReason reason;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /**
     * Created only through {@link Employee}, which owns the rules about what a valid revision is.
     */
    SalaryRevision(Employee employee, Money salary, LocalDate effectiveDate, RevisionReason reason) {
        if (salary == null || salary.isZero()) {
            throw new IllegalArgumentException("a salary revision needs a positive amount");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effective date is required");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason is required");
        }
        this.employee = employee;
        this.salary = salary;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.recordedAt = Instant.now();
    }

    boolean isEffectiveOn(LocalDate date) {
        return !effectiveDate.isAfter(date);
    }

    @Override
    public String toString() {
        return "SalaryRevision[" + salary + " from " + effectiveDate + " (" + reason + ")]";
    }
}
