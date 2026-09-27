package com.acme.salary.service;

import com.acme.salary.dto.EmployeeCriteria;
import com.acme.salary.dto.CsvExport;
import com.acme.salary.dto.EmployeeDetail;
import com.acme.salary.dto.EmployeeFilter;
import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.ExitRequest;
import com.acme.salary.dto.HireEmployeeRequest;
import com.acme.salary.dto.MoneyView;
import com.acme.salary.dto.PromoteRequest;
import com.acme.salary.dto.RecordRevisionRequest;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.model.Employee;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.repository.CurrentSalaryRow;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.EmployeeSpecifications;
import com.acme.salary.repository.SalaryRevisionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The operations the HR Manager can perform, in terms the UI can call.
 *
 * <p>The rules about what a valid pay change is live in {@link Employee}; this class is about
 * loading the right things, saying what was not found, and assembling what the API returns.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employees;
    private final SalaryRevisionRepository revisions;
    private final EmployeeCsvWriter csvWriter;
    private final Clock clock;

    /** Exports come out in a stable order, so two exports of the same data can be compared. */
    private static final Sort EXPORT_ORDER = Sort.by("employeeCode");

    /**
     * One page of employees with the salary each was on as at the filter date.
     *
     * <p>Two queries, never more: one for the page of employees, one for the salaries of just those
     * employees. Fetching the revisions along with the employees would make Hibernate paginate in
     * memory, which at 10,000 rows means loading all of them to show 25.
     */
    public Page<EmployeeSummary> search(EmployeeCriteria criteria, Pageable pageable) {
        EmployeeFilter filter = criteria.asOf(today());
        Page<Employee> page = employees.findAll(EmployeeSpecifications.matching(filter), pageable);
        if (page.isEmpty()) {
            return page.map(employee -> toSummary(employee, null, filter.asOf()));
        }

        Map<Long, CurrentSalaryRow> salaries = currentSalariesByEmployee(page.getContent(), filter.asOf());
        return page.map(employee -> toSummary(employee, salaries.get(employee.getId()), filter.asOf()));
    }

    /** Employees paired with the salary each was on, in one extra query rather than one per row. */
    private List<EmployeeSummary> withCurrentSalaries(List<Employee> employees, LocalDate asOf) {
        if (employees.isEmpty()) {
            return List.of();
        }
        Map<Long, CurrentSalaryRow> salaries = currentSalariesByEmployee(employees, asOf);
        return employees.stream()
                .map(employee -> toSummary(employee, salaries.get(employee.getId()), asOf))
                .toList();
    }

    private Map<Long, CurrentSalaryRow> currentSalariesByEmployee(List<Employee> employees, LocalDate asOf) {
        return revisions.findCurrentSalaries(employees.stream().map(Employee::getId).toList(), asOf)
                .stream()
                .collect(Collectors.toMap(CurrentSalaryRow::getEmployeeId, Function.identity()));
    }

    /**
     * Every employee matching the filter, as a CSV file. Not paged, because an export of one page
     * is not an export.
     *
     * <p>The whole result is built in memory. At the sizes this system is for that is measured in
     * megabytes; if exports ever had to cover far more than 10,000 people, this is the method that
     * would stream instead.
     */
    public CsvExport exportCsv(EmployeeCriteria criteria) {
        LocalDate asOf = today();
        List<Employee> matching = employees.findAll(
                EmployeeSpecifications.matching(criteria.asOf(asOf)), EXPORT_ORDER);

        List<EmployeeSummary> rows = withCurrentSalaries(matching, asOf);
        return new CsvExport("employees-" + asOf + ".csv", csvWriter.write(rows));
    }

    public EmployeeDetail findDetail(Long id) {
        return EmployeeDetail.of(require(id), today());
    }

    @Transactional
    public EmployeeDetail hire(HireEmployeeRequest request) {
        Employee employee = Employee.hire(
                request.employeeCode(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.country(),
                request.department(),
                request.jobLevel(),
                request.hireDate(),
                Money.of(request.salaryAmount(), request.currency()));

        return EmployeeDetail.of(employees.save(employee), today());
    }

    @Transactional
    public EmployeeDetail updateDetails(Long id, UpdateEmployeeRequest request) {
        Employee employee = require(id);
        employee.correctName(request.firstName(), request.lastName());
        employee.correctEmail(request.email());
        employee.changeDepartment(request.department());
        return EmployeeDetail.of(employee, today());
    }

    @Transactional
    public EmployeeDetail recordRevision(Long id, RecordRevisionRequest request) {
        if (request.reason() == RevisionReason.PROMOTION) {
            throw new IllegalArgumentException(
                    "record a promotion through the promotion endpoint, so the level moves with the pay");
        }
        Employee employee = require(id);
        employee.recordRevision(
                Money.of(request.amount(), request.currency()), request.effectiveDate(), request.reason());
        return EmployeeDetail.of(employee, today());
    }

    @Transactional
    public EmployeeDetail promote(Long id, PromoteRequest request) {
        Employee employee = require(id);
        employee.promoteTo(
                request.newLevel(), Money.of(request.amount(), request.currency()), request.effectiveDate());
        return EmployeeDetail.of(employee, today());
    }

    @Transactional
    public EmployeeDetail markExit(Long id, ExitRequest request) {
        Employee employee = require(id);
        employee.markExit(request.lastWorkingDay());
        return EmployeeDetail.of(employee, today());
    }

    private Employee require(Long id) {
        return employees.findWithRevisionsById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static EmployeeSummary toSummary(Employee employee, CurrentSalaryRow salary, LocalDate asOf) {
        return new EmployeeSummary(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.fullName(),
                employee.getEmail(),
                employee.getCountry(),
                employee.getDepartment(),
                employee.getJobLevel(),
                employee.getHireDate(),
                employee.getExitDate(),
                employee.isActiveOn(asOf),
                salary == null ? null : new MoneyView(salary.getAmount(), salary.getCurrency()),
                salary == null ? null : salary.getEffectiveDate());
    }

    /** Exposed for the seed and reporting code that needs the same "as at" rule. */
    public List<CurrentSalaryRow> currentSalaries(List<Long> employeeIds, LocalDate asOf) {
        return employeeIds.isEmpty() ? List.of() : revisions.findCurrentSalaries(employeeIds, asOf);
    }
}
