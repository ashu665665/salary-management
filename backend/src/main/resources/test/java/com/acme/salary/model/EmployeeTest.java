package com.acme.salary.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private static final LocalDate HIRED = LocalDate.of(2022, 4, 1);
    private static final Money STARTING_SALARY = Money.of("1200000", "INR");

    private static Employee anEmployee() {
        return Employee.hire(
                "ACME-000001",
                "Priya",
                "Nair",
                "priya.nair@acme.example",
                Country.INDIA,
                Department.ENGINEERING,
                JobLevel.MID,
                HIRED,
                STARTING_SALARY);
    }

    @Nested
    class Hiring {

        @Test
        void starts_with_one_revision_dated_the_hire_date() {
            Employee employee = anEmployee();

            assertThat(employee.getRevisions()).hasSize(1);
            assertThat(employee.latestRevision().getReason()).isEqualTo(RevisionReason.HIRE);
            assertThat(employee.latestRevision().getEffectiveDate()).isEqualTo(HIRED);
            assertThat(employee.salaryOn(HIRED)).contains(STARTING_SALARY);
        }

        @Test
        void trims_whitespace_from_names() {
            Employee employee = Employee.hire(
                    "ACME-000002", "  Jonas ", " Weber ", "jonas@acme.example",
                    Country.GERMANY, Department.SALES, JobLevel.SENIOR, HIRED, Money.of("85000", "EUR"));

            assertThat(employee.fullName()).isEqualTo("Jonas Weber");
        }

        @Test
        void requires_a_name() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Employee.hire(
                            "ACME-000003", " ", "Weber", "jonas2@acme.example",
                            Country.GERMANY, Department.SALES, JobLevel.SENIOR, HIRED, Money.of("85000", "EUR")))
                    .withMessageContaining("first name is required");
        }

        @Test
        void requires_a_positive_starting_salary() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Employee.hire(
                            "ACME-000004", "Ana", "Silva", "ana@acme.example",
                            Country.BRAZIL, Department.FINANCE, JobLevel.MID, HIRED, Money.of("0", "BRL")))
                    .withMessageContaining("positive amount");
        }
    }

    @Nested
    class RecordingARevision {

        @Test
        void keeps_the_previous_revision() {
            Employee employee = anEmployee();

            employee.recordRevision(Money.of("1400000", "INR"), LocalDate.of(2023, 4, 1), RevisionReason.ANNUAL_RAISE);

            assertThat(employee.getRevisions()).hasSize(2);
            assertThat(employee.salaryOn(HIRED)).contains(STARTING_SALARY);
        }

        @Test
        void rejects_a_date_before_the_hire_date() {
            Employee employee = anEmployee();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.recordRevision(
                            Money.of("1300000", "INR"), HIRED.minusDays(1), RevisionReason.ANNUAL_RAISE))
                    .withMessageContaining("before the hire date");
        }

        @Test
        void rejects_a_second_revision_on_the_same_date() {
            Employee employee = anEmployee();
            LocalDate raiseDate = LocalDate.of(2023, 4, 1);
            employee.recordRevision(Money.of("1400000", "INR"), raiseDate, RevisionReason.ANNUAL_RAISE);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.recordRevision(
                            Money.of("1450000", "INR"), raiseDate, RevisionReason.MARKET_CORRECTION))
                    .withMessageContaining("already exists");
        }

        @Test
        void rejects_a_second_hire_revision() {
            Employee employee = anEmployee();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.recordRevision(
                            Money.of("1400000", "INR"), LocalDate.of(2023, 4, 1), RevisionReason.HIRE))
                    .withMessageContaining("created when the employee is hired");
        }

        @Test
        void keeps_revisions_in_date_order_even_when_recorded_out_of_order() {
            Employee employee = anEmployee();

            employee.recordRevision(Money.of("1600000", "INR"), LocalDate.of(2024, 4, 1), RevisionReason.ANNUAL_RAISE);
            employee.recordRevision(Money.of("1400000", "INR"), LocalDate.of(2023, 4, 1), RevisionReason.ANNUAL_RAISE);

            assertThat(employee.getRevisions())
                    .extracting(SalaryRevision::getEffectiveDate)
                    .containsExactly(HIRED, LocalDate.of(2023, 4, 1), LocalDate.of(2024, 4, 1));
        }

        @Test
        void exposes_revisions_as_an_unmodifiable_list() {
            Employee employee = anEmployee();

            assertThat(employee.getRevisions()).isUnmodifiable();
        }
    }

    @Nested
    class SalaryOnADate {

        @Test
        void is_empty_before_the_employee_joined() {
            assertThat(anEmployee().salaryOn(HIRED.minusDays(1))).isEmpty();
        }

        @Test
        void is_the_most_recent_revision_not_after_that_date() {
            Employee employee = anEmployee();
            employee.recordRevision(Money.of("1400000", "INR"), LocalDate.of(2023, 4, 1), RevisionReason.ANNUAL_RAISE);
            employee.recordRevision(Money.of("1600000", "INR"), LocalDate.of(2024, 4, 1), RevisionReason.ANNUAL_RAISE);

            assertThat(employee.salaryOn(LocalDate.of(2023, 12, 31))).contains(Money.of("1400000", "INR"));
            assertThat(employee.salaryOn(LocalDate.of(2024, 4, 1))).contains(Money.of("1600000", "INR"));
        }

        @Test
        void ignores_a_revision_dated_in_the_future() {
            Employee employee = anEmployee();
            LocalDate today = LocalDate.of(2024, 1, 1);
            employee.recordRevision(Money.of("1600000", "INR"), today.plusMonths(3), RevisionReason.ANNUAL_RAISE);

            assertThat(employee.salaryOn(today)).contains(STARTING_SALARY);
            assertThat(employee.latestRevision().getSalary()).isEqualTo(Money.of("1600000", "INR"));
        }
    }

    @Nested
    class Promotion {

        @Test
        void moves_the_level_and_records_the_pay_change_together() {
            Employee employee = anEmployee();
            LocalDate effective = LocalDate.of(2024, 1, 1);

            employee.promoteTo(JobLevel.SENIOR, Money.of("1800000", "INR"), effective);

            assertThat(employee.getJobLevel()).isEqualTo(JobLevel.SENIOR);
            assertThat(employee.salaryOn(effective)).contains(Money.of("1800000", "INR"));
            assertThat(employee.latestRevision().getReason()).isEqualTo(RevisionReason.PROMOTION);
        }

        @Test
        void rejects_a_move_to_a_level_that_is_not_higher() {
            Employee employee = anEmployee();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.promoteTo(
                            JobLevel.JUNIOR, Money.of("1800000", "INR"), LocalDate.of(2024, 1, 1)))
                    .withMessageContaining("must be to a higher level");
        }

        @Test
        void leaves_the_level_unchanged_when_the_pay_change_is_invalid() {
            Employee employee = anEmployee();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.promoteTo(
                            JobLevel.SENIOR, Money.of("1800000", "INR"), HIRED.minusDays(1)));

            assertThat(employee.getJobLevel()).isEqualTo(JobLevel.MID);
        }
    }

    @Nested
    class Leaving {

        @Test
        void records_the_last_working_day_and_keeps_the_history() {
            Employee employee = anEmployee();
            LocalDate lastDay = LocalDate.of(2024, 6, 30);

            employee.markExit(lastDay);

            assertThat(employee.hasLeft()).isTrue();
            assertThat(employee.getExitDate()).isEqualTo(lastDay);
            assertThat(employee.getRevisions()).isNotEmpty();
        }

        @Test
        void counts_as_employed_on_the_last_working_day_itself() {
            Employee employee = anEmployee();
            LocalDate lastDay = LocalDate.of(2024, 6, 30);
            employee.markExit(lastDay);

            assertThat(employee.isActiveOn(lastDay)).isTrue();
            assertThat(employee.isActiveOn(lastDay.plusDays(1))).isFalse();
        }

        @Test
        void is_not_employed_before_the_hire_date() {
            assertThat(anEmployee().isActiveOn(HIRED.minusDays(1))).isFalse();
            assertThat(anEmployee().isActiveOn(HIRED)).isTrue();
        }

        @Test
        void rejects_an_exit_before_the_hire_date() {
            Employee employee = anEmployee();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.markExit(HIRED.minusDays(1)))
                    .withMessageContaining("before the hire date");
        }

        @Test
        void rejects_an_exit_that_would_leave_a_later_pay_rise_stranded() {
            Employee employee = anEmployee();
            employee.recordRevision(Money.of("1400000", "INR"), LocalDate.of(2024, 4, 1), RevisionReason.ANNUAL_RAISE);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.markExit(LocalDate.of(2024, 1, 31)))
                    .withMessageContaining("after the exit date");
        }

        @Test
        void rejects_a_pay_change_after_the_exit_date() {
            Employee employee = anEmployee();
            employee.markExit(LocalDate.of(2024, 6, 30));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.recordRevision(
                            Money.of("1500000", "INR"), LocalDate.of(2024, 7, 1), RevisionReason.ANNUAL_RAISE))
                    .withMessageContaining("after the exit date");
        }

        @Test
        void cannot_leave_twice() {
            Employee employee = anEmployee();
            employee.markExit(LocalDate.of(2024, 6, 30));

            assertThatIllegalStateException()
                    .isThrownBy(() -> employee.markExit(LocalDate.of(2024, 7, 31)))
                    .withMessageContaining("already left");
        }
    }
}
