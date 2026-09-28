package com.acme.salary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.model.SalaryRevision;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The seed data has to look like a real organisation, or the dashboard tells a story that is not
 * worth reading. These tests pin down the properties that make it believable.
 */
class EmployeeDataFactoryTest {

    private static final LocalDate AS_OF = LocalDate.of(2025, 6, 1);
    private static final long SEED = 20250601L;

    private final EmployeeDataFactory factory = new EmployeeDataFactory();

    private List<Employee> generate(int count) {
        return factory.generate(count, AS_OF, SEED);
    }

    @Nested
    class Repeatability {

        @Test
        void produces_the_same_people_for_the_same_seed() {
            List<Employee> first = generate(200);
            List<Employee> second = generate(200);

            assertThat(summarise(first)).isEqualTo(summarise(second));
        }

        @Test
        void produces_different_people_for_a_different_seed() {
            assertThat(summarise(factory.generate(200, AS_OF, 1L)))
                    .isNotEqualTo(summarise(factory.generate(200, AS_OF, 2L)));
        }

        private List<String> summarise(List<Employee> employees) {
            return employees.stream()
                    .map(e -> e.getEmployeeCode() + "|" + e.fullName() + "|" + e.getCountry() + "|"
                            + e.getJobLevel() + "|" + e.latestRevision().getSalary())
                    .toList();
        }
    }

    @Nested
    class Shape {

        @Test
        void generates_the_number_asked_for() {
            assertThat(generate(500)).hasSize(500);
        }

        @Test
        void gives_everyone_a_unique_code_and_email() {
            List<Employee> employees = generate(1000);

            assertThat(employees).extracting(Employee::getEmployeeCode).doesNotHaveDuplicates();
            assertThat(employees).extracting(Employee::getEmail).doesNotHaveDuplicates();
        }

        @Test
        void covers_every_country_and_department() {
            List<Employee> employees = generate(1000);

            assertThat(employees).extracting(Employee::getCountry)
                    .containsAll(List.of(Country.values()));
            assertThat(employees).extracting(Employee::getDepartment)
                    .containsAll(List.of(Department.values()));
        }

        @Test
        void has_more_juniors_than_executives() {
            List<Employee> employees = generate(1000);

            long juniors = employees.stream().filter(e -> e.getJobLevel() == JobLevel.JUNIOR).count();
            long executives = employees.stream().filter(e -> e.getJobLevel() == JobLevel.EXECUTIVE).count();

            assertThat(juniors).isGreaterThan(executives);
        }

        @Test
        void includes_some_leavers_but_keeps_most_people() {
            List<Employee> employees = generate(1000);

            long leavers = employees.stream().filter(Employee::hasLeft).count();

            assertThat(leavers).isPositive();
            assertThat(leavers).isLessThan(200);
        }
    }

    @Nested
    class PayHistory {

        @Test
        void starts_everyone_with_a_hire_revision_on_their_hire_date() {
            for (Employee employee : generate(300)) {
                SalaryRevision first = employee.getRevisions().get(0);
                assertThat(first.getReason()).isEqualTo(RevisionReason.HIRE);
                assertThat(first.getEffectiveDate()).isEqualTo(employee.getHireDate());
            }
        }

        @Test
        void pays_everyone_in_the_currency_of_their_country() {
            for (Employee employee : generate(300)) {
                assertThat(employee.getRevisions())
                        .allSatisfy(revision -> assertThat(revision.getSalary().getCurrency())
                                .isEqualTo(employee.getCountry().getPayCurrency()));
            }
        }

        @Test
        void never_dates_a_revision_before_someone_joined_or_after_they_left() {
            for (Employee employee : generate(500)) {
                for (SalaryRevision revision : employee.getRevisions()) {
                    assertThat(revision.getEffectiveDate()).isAfterOrEqualTo(employee.getHireDate());
                    if (employee.hasLeft()) {
                        assertThat(revision.getEffectiveDate()).isBeforeOrEqualTo(employee.getExitDate());
                    }
                }
            }
        }

        @Test
        void never_dates_a_revision_in_the_future() {
            for (Employee employee : generate(500)) {
                assertThat(employee.latestRevision().getEffectiveDate()).isBeforeOrEqualTo(AS_OF);
            }
        }

        @Test
        void gives_long_serving_people_more_than_one_revision() {
            // Leavers are excluded on purpose: someone who joined long ago but left within a year
            // never reached a review round, and one revision is the right answer for them.
            List<Employee> longServing = generate(500).stream()
                    .filter(e -> !e.hasLeft())
                    .filter(e -> e.getHireDate().isBefore(AS_OF.minusYears(4)))
                    .toList();

            assertThat(longServing).isNotEmpty();
            assertThat(longServing).allSatisfy(e -> assertThat(e.getRevisions()).hasSizeGreaterThan(1));
        }

        @Test
        void raises_pay_over_time_rather_than_lowering_it() {
            for (Employee employee : generate(300)) {
                assertThat(employee.latestRevision().getSalary())
                        .isGreaterThanOrEqualTo(employee.getRevisions().get(0).getSalary());
            }
        }

        @Test
        void promotes_some_people_and_moves_their_level_with_the_pay() {
            List<Employee> promoted = generate(500).stream()
                    .filter(e -> e.getRevisions().stream()
                            .anyMatch(r -> r.getReason() == RevisionReason.PROMOTION))
                    .toList();

            assertThat(promoted).isNotEmpty();
            assertThat(promoted).allSatisfy(e -> assertThat(e.getJobLevel()).isNotEqualTo(JobLevel.JUNIOR));
        }
    }

    @Nested
    class Realism {

        @Test
        void pays_senior_people_more_than_juniors_in_the_same_country() {
            List<Employee> indians = generate(2000).stream()
                    .filter(e -> e.getCountry() == Country.INDIA)
                    .toList();

            double juniorAverage = averageSalary(indians, JobLevel.JUNIOR);
            double directorAverage = averageSalary(indians, JobLevel.DIRECTOR);

            assertThat(directorAverage).isGreaterThan(juniorAverage);
        }

        private double averageSalary(List<Employee> employees, JobLevel level) {
            return employees.stream()
                    .filter(e -> e.getJobLevel() == level)
                    .mapToDouble(e -> e.latestRevision().getSalary().getAmount().doubleValue())
                    .average()
                    .orElseThrow();
        }
    }
}
