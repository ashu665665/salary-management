package com.acme.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.salary.dto.EmployeeCriteria;
import com.acme.salary.dto.EmployeeDetail;
import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.ExitRequest;
import com.acme.salary.dto.HireEmployeeRequest;
import com.acme.salary.dto.PromoteRequest;
import com.acme.salary.dto.RecordRevisionRequest;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.repository.CurrentSalaryRow;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRevisionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);

    @Mock
    private EmployeeRepository employees;

    @Mock
    private SalaryRevisionRepository revisions;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new EmployeeService(employees, revisions, fixedClock);
    }

    private static Employee anEmployee(Long id) {
        Employee employee = Employee.hire(
                "ACME-1", "Priya", "Nair", "priya@acme.example",
                Country.INDIA, Department.ENGINEERING, JobLevel.MID,
                LocalDate.of(2020, 1, 1), Money.of("1200000", "INR"));
        if (id != null) {
            setId(employee, id);
        }
        return employee;
    }

    /** The id is database-assigned, so a test that needs one has to put it there. */
    private static void setId(Employee employee, Long id) {
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CurrentSalaryRow salaryRow(Long employeeId, String amount, String currency, LocalDate from) {
        return new CurrentSalaryRow() {
            @Override
            public Long getEmployeeId() {
                return employeeId;
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }

            @Override
            public String getCurrency() {
                return currency;
            }

            @Override
            public LocalDate getEffectiveDate() {
                return from;
            }
        };
    }

    @Nested
    class Searching {

        @Test
        void puts_the_current_salary_onto_each_row() {
            Employee employee = anEmployee(7L);
            when(employees.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(employee), PageRequest.of(0, 20), 1));
            when(revisions.findCurrentSalaries(anyCollection(), any(LocalDate.class)))
                    .thenReturn(List.of(salaryRow(7L, "1600000", "INR", LocalDate.of(2024, 4, 1))));

            Page<EmployeeSummary> page = service.search(EmployeeCriteria.unfiltered(), PageRequest.of(0, 20));

            assertThat(page.getContent()).singleElement().satisfies(summary -> {
                assertThat(summary.fullName()).isEqualTo("Priya Nair");
                assertThat(summary.currentSalary().amount()).isEqualByComparingTo("1600000");
                assertThat(summary.currentSalary().currency()).isEqualTo("INR");
                assertThat(summary.salaryEffectiveFrom()).isEqualTo(LocalDate.of(2024, 4, 1));
                assertThat(summary.active()).isTrue();
            });
        }

        @Test
        void leaves_the_salary_empty_when_there_is_no_revision_in_force() {
            Employee employee = anEmployee(7L);
            when(employees.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(employee), PageRequest.of(0, 20), 1));
            when(revisions.findCurrentSalaries(anyCollection(), any(LocalDate.class))).thenReturn(List.of());

            Page<EmployeeSummary> page = service.search(EmployeeCriteria.unfiltered(), PageRequest.of(0, 20));

            assertThat(page.getContent()).singleElement()
                    .satisfies(summary -> assertThat(summary.currentSalary()).isNull());
        }

        @Test
        void does_not_ask_for_salaries_when_the_page_is_empty() {
            when(employees.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<EmployeeSummary> page = service.search(EmployeeCriteria.unfiltered(), PageRequest.of(0, 20));

            assertThat(page).isEmpty();
            verify(revisions, never()).findCurrentSalaries(anyList(), any(LocalDate.class));
        }
    }

    @Nested
    class ReadingOne {

        @Test
        void returns_the_employee_with_their_history() {
            when(employees.findWithRevisionsById(7L)).thenReturn(Optional.of(anEmployee(7L)));

            EmployeeDetail detail = service.findDetail(7L);

            assertThat(detail.employeeCode()).isEqualTo("ACME-1");
            assertThat(detail.revisions()).hasSize(1);
            assertThat(detail.currentSalary().amount()).isEqualByComparingTo("1200000");
        }

        @Test
        void complains_when_there_is_no_such_employee() {
            when(employees.findWithRevisionsById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findDetail(404L))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining("404");
        }
    }

    @Nested
    class Hiring {

        @Test
        void saves_the_new_employee_with_their_starting_salary() {
            when(employees.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.hire(new HireEmployeeRequest(
                    "ACME-9", "Dana", "Brooks", "dana@acme.example",
                    Country.UNITED_STATES, Department.SALES, JobLevel.MANAGER,
                    LocalDate.of(2025, 1, 6), new BigDecimal("150000"), "USD"));

            ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
            verify(employees).save(saved.capture());
            assertThat(saved.getValue().getEmployeeCode()).isEqualTo("ACME-9");
            assertThat(saved.getValue().getRevisions()).singleElement()
                    .satisfies(revision -> {
                        assertThat(revision.getReason()).isEqualTo(RevisionReason.HIRE);
                        assertThat(revision.getSalary()).isEqualTo(Money.of("150000", "USD"));
                    });
        }
    }

    @Nested
    class ChangingPay {

        @Test
        void records_a_raise_against_the_employee() {
            Employee employee = anEmployee(7L);
            when(employees.findWithRevisionsById(7L)).thenReturn(Optional.of(employee));

            service.recordRevision(7L, new RecordRevisionRequest(
                    new BigDecimal("1400000"), "INR", LocalDate.of(2024, 4, 1), RevisionReason.ANNUAL_RAISE));

            assertThat(employee.getRevisions()).hasSize(2);
            assertThat(employee.latestRevision().getReason()).isEqualTo(RevisionReason.ANNUAL_RAISE);
        }

        @Test
        void refuses_to_record_a_promotion_through_the_raise_endpoint() {
            // Rejected before the employee is even loaded: a promotion has to move the level too.
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.recordRevision(7L, new RecordRevisionRequest(
                            new BigDecimal("1400000"), "INR", LocalDate.of(2024, 4, 1), RevisionReason.PROMOTION)))
                    .withMessageContaining("promotion");
        }

        @Test
        void promotes_an_employee_to_a_higher_level() {
            Employee employee = anEmployee(7L);
            when(employees.findWithRevisionsById(7L)).thenReturn(Optional.of(employee));

            EmployeeDetail detail = service.promote(7L, new PromoteRequest(
                    JobLevel.SENIOR, new BigDecimal("1800000"), "INR", LocalDate.of(2024, 4, 1)));

            assertThat(detail.jobLevel()).isEqualTo(JobLevel.SENIOR);
            assertThat(employee.latestRevision().getReason()).isEqualTo(RevisionReason.PROMOTION);
        }
    }

    @Nested
    class Leaving {

        @Test
        void records_the_last_working_day() {
            Employee employee = anEmployee(7L);
            when(employees.findWithRevisionsById(7L)).thenReturn(Optional.of(employee));

            EmployeeDetail detail = service.markExit(7L, new ExitRequest(LocalDate.of(2025, 5, 31)));

            assertThat(detail.exitDate()).isEqualTo(LocalDate.of(2025, 5, 31));
            assertThat(detail.active()).isFalse();
        }
    }

    @Nested
    class Correcting {

        @Test
        void updates_the_details_that_are_allowed_to_change() {
            Employee employee = anEmployee(7L);
            when(employees.findWithRevisionsById(7L)).thenReturn(Optional.of(employee));

            EmployeeDetail detail = service.updateDetails(7L, new UpdateEmployeeRequest(
                    "Priya", "Nair-Kumar", "priya.kumar@acme.example", Department.PRODUCT));

            assertThat(detail.lastName()).isEqualTo("Nair-Kumar");
            assertThat(detail.email()).isEqualTo("priya.kumar@acme.example");
            assertThat(detail.department()).isEqualTo(Department.PRODUCT);
        }
    }
}
