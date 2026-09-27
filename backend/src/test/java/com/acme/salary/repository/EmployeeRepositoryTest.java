package com.acme.salary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.dto.EmployeeFilter;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.model.SalaryRevision;
import com.acme.salary.support.PostgresTestBase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class EmployeeRepositoryTest extends PostgresTestBase {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private SalaryRevisionRepository revisions;

    @BeforeEach
    void clearPreviousData() {
        employees.deleteAll();
    }

    private Employee hire(String code, String first, String last, Country country, Department department,
            JobLevel level, String amount) {
        return Employee.hire(
                code, first, last, code.toLowerCase() + "@acme.example",
                country, department, level, LocalDate.of(2020, 1, 1),
                Money.of(amount, country.getPayCurrency()));
    }

    @Nested
    class Persistence {

        @Test
        void saves_an_employee_together_with_the_hire_revision() {
            Employee saved = employees.save(
                    hire("ACME-1", "Priya", "Nair", Country.INDIA, Department.ENGINEERING, JobLevel.MID, "1200000"));

            Optional<Employee> found = employees.findWithRevisionsById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getRevisions()).hasSize(1);
            assertThat(found.get().getRevisions().get(0).getSalary())
                    .isEqualTo(Money.of("1200000", "INR"));
        }

        @Test
        void finds_an_employee_by_their_code() {
            employees.save(hire("ACME-2", "Jonas", "Weber", Country.GERMANY, Department.SALES, JobLevel.SENIOR, "85000"));

            assertThat(employees.findByEmployeeCode("ACME-2")).isPresent();
            assertThat(employees.findByEmployeeCode("ACME-999")).isEmpty();
        }

        @Test
        void keeps_the_whole_history_when_more_revisions_are_added() {
            Employee employee = hire("ACME-3", "Ana", "Silva", Country.BRAZIL, Department.FINANCE, JobLevel.MID, "90000");
            employee.recordRevision(Money.of("99000", "BRL"), LocalDate.of(2023, 1, 1), RevisionReason.ANNUAL_RAISE);
            Employee saved = employees.saveAndFlush(employee);

            assertThat(employees.findWithRevisionsById(saved.getId()).orElseThrow().getRevisions())
                    .extracting(SalaryRevision::getEffectiveDate)
                    .containsExactly(LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1));
        }
    }

    @Nested
    class Filtering {

        @BeforeEach
        void setUpPeople() {
            employees.save(hire("ACME-IN-1", "Priya", "Nair", Country.INDIA, Department.ENGINEERING, JobLevel.MID, "1200000"));
            employees.save(hire("ACME-DE-1", "Jonas", "Weber", Country.GERMANY, Department.ENGINEERING, JobLevel.SENIOR, "85000"));
            employees.save(hire("ACME-US-1", "Dana", "Brooks", Country.UNITED_STATES, Department.SALES, JobLevel.MANAGER, "150000"));
            employees.flush();
        }

        private Page<Employee> search(EmployeeFilter filter) {
            return employees.findAll(EmployeeSpecifications.matching(filter), PageRequest.of(0, 20));
        }

        @Test
        void returns_everyone_when_nothing_is_filtered() {
            assertThat(search(EmployeeFilter.activeOn(TODAY))).hasSize(3);
        }

        @Test
        void filters_by_country() {
            Page<Employee> found = search(new EmployeeFilter(null, Country.INDIA, null, null, false, TODAY));

            assertThat(found).extracting(Employee::getEmployeeCode).containsExactly("ACME-IN-1");
        }

        @Test
        void filters_by_department() {
            Page<Employee> found = search(new EmployeeFilter(null, null, Department.ENGINEERING, null, false, TODAY));

            assertThat(found).extracting(Employee::getEmployeeCode)
                    .containsExactlyInAnyOrder("ACME-IN-1", "ACME-DE-1");
        }

        @Test
        void filters_by_job_level() {
            Page<Employee> found = search(new EmployeeFilter(null, null, null, JobLevel.MANAGER, false, TODAY));

            assertThat(found).extracting(Employee::getEmployeeCode).containsExactly("ACME-US-1");
        }

        @Test
        void combines_filters() {
            Page<Employee> found = search(
                    new EmployeeFilter(null, Country.GERMANY, Department.SALES, null, false, TODAY));

            assertThat(found).isEmpty();
        }

        @Test
        void searches_names_without_caring_about_case() {
            Page<Employee> found = search(new EmployeeFilter("pRiYa", null, null, null, false, TODAY));

            assertThat(found).extracting(Employee::getEmployeeCode).containsExactly("ACME-IN-1");
        }

        @Test
        void searches_on_part_of_a_last_name() {
            assertThat(search(new EmployeeFilter("web", null, null, null, false, TODAY)))
                    .extracting(Employee::getEmployeeCode)
                    .containsExactly("ACME-DE-1");
        }

        @Test
        void searches_on_employee_code_and_email() {
            assertThat(search(new EmployeeFilter("ACME-US", null, null, null, false, TODAY))).hasSize(1);
            assertThat(search(new EmployeeFilter("acme-de-1@acme.example", null, null, null, false, TODAY))).hasSize(1);
        }
    }

    @Nested
    class Leavers {

        @Test
        void are_left_out_by_default() {
            Employee leaver = hire("ACME-OUT", "Sam", "Cole", Country.CANADA, Department.LEGAL, JobLevel.SENIOR, "95000");
            leaver.markExit(LocalDate.of(2024, 12, 31));
            employees.saveAndFlush(leaver);

            assertThat(employees.findAll(EmployeeSpecifications.matching(EmployeeFilter.activeOn(TODAY)),
                    PageRequest.of(0, 20))).isEmpty();
        }

        @Test
        void are_included_when_asked_for() {
            Employee leaver = hire("ACME-OUT", "Sam", "Cole", Country.CANADA, Department.LEGAL, JobLevel.SENIOR, "95000");
            leaver.markExit(LocalDate.of(2024, 12, 31));
            employees.saveAndFlush(leaver);

            Page<Employee> found = employees.findAll(
                    EmployeeSpecifications.matching(new EmployeeFilter(null, null, null, null, true, TODAY)),
                    PageRequest.of(0, 20));

            assertThat(found).hasSize(1);
        }

        @Test
        void still_count_as_employed_when_their_last_day_has_not_arrived() {
            Employee leaving = hire("ACME-NOTICE", "Mia", "Tan", Country.SINGAPORE, Department.PRODUCT, JobLevel.LEAD, "120000");
            leaving.markExit(TODAY.plusMonths(1));
            employees.saveAndFlush(leaving);

            assertThat(employees.findAll(EmployeeSpecifications.matching(EmployeeFilter.activeOn(TODAY)),
                    PageRequest.of(0, 20))).hasSize(1);
        }
    }

    @Nested
    class Paging {

        @BeforeEach
        void setUpManyPeople() {
            for (int i = 1; i <= 25; i++) {
                employees.save(hire("ACME-P" + i, "Person" + i, "Surname" + String.format("%02d", i),
                        Country.POLAND, Department.OPERATIONS, JobLevel.MID, "90000"));
            }
            employees.flush();
        }

        @Test
        void returns_one_page_at_a_time_with_the_total_count() {
            Page<Employee> page = employees.findAll(
                    EmployeeSpecifications.matching(EmployeeFilter.activeOn(TODAY)),
                    PageRequest.of(0, 10, Sort.by("lastName")));

            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(25);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        void sorts_by_the_requested_column() {
            Page<Employee> page = employees.findAll(
                    EmployeeSpecifications.matching(EmployeeFilter.activeOn(TODAY)),
                    PageRequest.of(0, 3, Sort.by("lastName").descending()));

            assertThat(page.getContent()).extracting(Employee::getLastName)
                    .containsExactly("Surname25", "Surname24", "Surname23");
        }
    }

    @Nested
    class CurrentSalaries {

        @Test
        void returns_the_latest_revision_on_or_before_the_date_for_each_employee() {
            Employee first = hire("ACME-S1", "Priya", "Nair", Country.INDIA, Department.ENGINEERING, JobLevel.MID, "1200000");
            first.recordRevision(Money.of("1400000", "INR"), LocalDate.of(2023, 4, 1), RevisionReason.ANNUAL_RAISE);
            first.recordRevision(Money.of("1600000", "INR"), LocalDate.of(2024, 4, 1), RevisionReason.ANNUAL_RAISE);
            Employee second = hire("ACME-S2", "Jonas", "Weber", Country.GERMANY, Department.SALES, JobLevel.SENIOR, "85000");
            employees.saveAll(List.of(first, second));
            employees.flush();

            List<CurrentSalaryRow> rows = revisions.findCurrentSalaries(
                    List.of(first.getId(), second.getId()), TODAY);

            assertThat(rows).hasSize(2);
            assertThat(rows).anySatisfy(row -> {
                assertThat(row.getEmployeeId()).isEqualTo(first.getId());
                assertThat(row.getAmount()).isEqualByComparingTo("1600000");
                assertThat(row.getCurrency()).isEqualTo("INR");
                assertThat(row.getEffectiveDate()).isEqualTo(LocalDate.of(2024, 4, 1));
            });
        }

        @Test
        void ignores_a_revision_dated_after_the_date_asked_for() {
            Employee employee = hire("ACME-S3", "Dana", "Brooks", Country.UNITED_STATES, Department.SALES, JobLevel.MANAGER, "150000");
            employee.recordRevision(Money.of("165000", "USD"), TODAY.plusMonths(2), RevisionReason.ANNUAL_RAISE);
            employees.saveAndFlush(employee);

            List<CurrentSalaryRow> rows = revisions.findCurrentSalaries(List.of(employee.getId()), TODAY);

            assertThat(rows).singleElement()
                    .satisfies(row -> assertThat(row.getAmount()).isEqualByComparingTo("150000"));
        }

        @Test
        void returns_nothing_for_an_employee_who_had_not_joined_yet() {
            Employee employee = hire("ACME-S4", "Mia", "Tan", Country.SINGAPORE, Department.PRODUCT, JobLevel.LEAD, "120000");
            employees.saveAndFlush(employee);

            assertThat(revisions.findCurrentSalaries(List.of(employee.getId()), LocalDate.of(2019, 1, 1))).isEmpty();
        }
    }
}
