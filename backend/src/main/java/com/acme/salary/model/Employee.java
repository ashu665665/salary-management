package com.acme.salary.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An employee and their pay history.
 *
 * <p>This is the only place salary revisions can be created, because the rules about what makes a
 * revision valid (not before the hire date, not after the exit date, one per date) only make sense
 * with the whole history in view.
 *
 * <p>Salary is deliberately not a field. It is derived from the revisions as at a date, so that
 * "what do they earn now", "what did they earn last April" and "what changed this year" are all the
 * same question asked with a different date.
 *
 * <p>Getters are generated, setters are not: every change goes through a method that says what
 * happened, such as {@link #promoteTo} or {@link #markExit}.
 */
@Entity
@Table(name = "employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "country", nullable = false, length = 40)
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 40)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_level", nullable = false, length = 40)
    private JobLevel jobLevel;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    /** The last day of employment. Null means still employed. */
    @Column(name = "exit_date")
    private LocalDate exitDate;

    /**
     * Not exposed by the generated getter: callers get an unmodifiable view from
     * {@link #getRevisions()}, so revisions can only be added through this class.
     */
    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("effectiveDate ASC")
    private List<SalaryRevision> revisions = new ArrayList<>();

    /**
     * Hires an employee at a starting salary. Every employee has a revision from the day they join,
     * so there is no such thing here as an employee with unknown pay.
     */
    public static Employee hire(
            String employeeCode,
            String firstName,
            String lastName,
            String email,
            Country country,
            Department department,
            JobLevel jobLevel,
            LocalDate hireDate,
            Money startingSalary) {

        Employee employee = new Employee();
        employee.employeeCode = requireText(employeeCode, "employee code");
        employee.firstName = requireText(firstName, "first name");
        employee.lastName = requireText(lastName, "last name");
        employee.email = requireText(email, "email");
        employee.country = Objects.requireNonNull(country, "country is required");
        employee.department = Objects.requireNonNull(department, "department is required");
        employee.jobLevel = Objects.requireNonNull(jobLevel, "job level is required");
        employee.hireDate = Objects.requireNonNull(hireDate, "hire date is required");
        employee.revisions.add(new SalaryRevision(employee, startingSalary, hireDate, RevisionReason.HIRE));
        return employee;
    }

    /**
     * Records a pay change. The previous revision is left untouched.
     */
    public SalaryRevision recordRevision(Money newSalary, LocalDate effectiveDate, RevisionReason reason) {
        Objects.requireNonNull(effectiveDate, "effective date is required");
        if (reason == RevisionReason.HIRE) {
            throw new IllegalArgumentException("the hire revision is created when the employee is hired");
        }
        if (effectiveDate.isBefore(hireDate)) {
            throw new IllegalArgumentException(
                    "effective date " + effectiveDate + " is before the hire date " + hireDate);
        }
        if (exitDate != null && effectiveDate.isAfter(exitDate)) {
            throw new IllegalArgumentException(
                    "effective date " + effectiveDate + " is after the exit date " + exitDate);
        }
        if (hasRevisionOn(effectiveDate)) {
            throw new IllegalArgumentException("a salary revision already exists for " + effectiveDate);
        }

        SalaryRevision revision = new SalaryRevision(this, newSalary, effectiveDate, reason);
        revisions.add(revision);
        revisions.sort(Comparator.comparing(SalaryRevision::getEffectiveDate));
        return revision;
    }

    /**
     * Moves the employee up a level and records the pay change that goes with it, because in
     * practice the two always happen together.
     *
     * <p>Only the current level is stored, not a history of levels. Level history would matter for
     * career reporting, which this system does not do.
     */
    public SalaryRevision promoteTo(JobLevel newLevel, Money newSalary, LocalDate effectiveDate) {
        Objects.requireNonNull(newLevel, "new level is required");
        if (!newLevel.isAbove(jobLevel)) {
            throw new IllegalArgumentException(
                    "promotion must be to a higher level than " + jobLevel + ", got " + newLevel);
        }
        SalaryRevision revision = recordRevision(newSalary, effectiveDate, RevisionReason.PROMOTION);
        this.jobLevel = newLevel;
        return revision;
    }

    /** Records a leaver. Their history is kept; they stop counting towards current payroll. */
    public void markExit(LocalDate lastWorkingDay) {
        Objects.requireNonNull(lastWorkingDay, "last working day is required");
        if (exitDate != null) {
            throw new IllegalStateException(employeeCode + " already left on " + exitDate);
        }
        if (lastWorkingDay.isBefore(hireDate)) {
            throw new IllegalArgumentException(
                    "exit date " + lastWorkingDay + " is before the hire date " + hireDate);
        }
        LocalDate lastRevision = latestRevision().getEffectiveDate();
        if (lastRevision.isAfter(lastWorkingDay)) {
            throw new IllegalArgumentException(
                    "there is a salary revision dated " + lastRevision + ", after the exit date " + lastWorkingDay);
        }
        this.exitDate = lastWorkingDay;
    }

    public void changeDepartment(Department newDepartment) {
        this.department = Objects.requireNonNull(newDepartment, "department is required");
    }

    public void correctName(String newFirstName, String newLastName) {
        this.firstName = requireText(newFirstName, "first name");
        this.lastName = requireText(newLastName, "last name");
    }

    public void correctEmail(String newEmail) {
        this.email = requireText(newEmail, "email");
    }

    /** What the employee earned on a given date, empty if they had not joined yet. */
    public Optional<Money> salaryOn(LocalDate date) {
        return revisionOn(date).map(SalaryRevision::getSalary);
    }

    public Optional<SalaryRevision> revisionOn(LocalDate date) {
        Objects.requireNonNull(date, "date is required");
        return revisions.stream()
                .filter(revision -> revision.isEffectiveOn(date))
                .max(Comparator.comparing(SalaryRevision::getEffectiveDate));
    }

    /**
     * The most recent revision by effective date, including one dated in the future. Use
     * {@link #revisionOn(LocalDate)} for what is actually in force on a given day.
     */
    public SalaryRevision latestRevision() {
        return revisions.get(revisions.size() - 1);
    }

    public boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date is required");
        return !date.isBefore(hireDate) && (exitDate == null || !date.isAfter(exitDate));
    }

    public boolean hasLeft() {
        return exitDate != null;
    }

    public List<SalaryRevision> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    private boolean hasRevisionOn(LocalDate date) {
        return revisions.stream().anyMatch(revision -> revision.getEffectiveDate().equals(date));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "Employee[" + employeeCode + " " + fullName() + "]";
    }
}
