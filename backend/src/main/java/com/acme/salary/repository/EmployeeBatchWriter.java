package com.acme.salary.repository;

import com.acme.salary.model.Employee;
import com.acme.salary.model.SalaryRevision;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bulk insert for seeding, deliberately bypassing JPA.
 *
 * <p>The entities use {@code GenerationType.IDENTITY}, which is the right choice for ordinary
 * writes but stops Hibernate batching inserts: it has to go to the database for each generated id.
 * At 10,000 employees and their revisions that is tens of thousands of round trips. Here the ids
 * are taken from the sequence up front, in one query, and everything else goes down in two batches.
 *
 * <p>This is the only place that writes rows without going through {@link Employee}, which is
 * acceptable because the objects it is handed were built by that class in the first place.
 */
@Repository
@RequiredArgsConstructor
public class EmployeeBatchWriter {

    private static final int BATCH_SIZE = 1_000;

    private static final String INSERT_EMPLOYEE = """
            insert into employee
                (id, employee_code, first_name, last_name, email, country, department, job_level, hire_date, exit_date)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_REVISION = """
            insert into salary_revision
                (employee_id, amount, currency, effective_date, reason, recorded_at)
            values (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public void write(List<Employee> employees) {
        if (employees.isEmpty()) {
            return;
        }
        List<Long> ids = reserveIds(employees.size());
        insertEmployees(employees, ids);
        insertRevisions(employees, ids);
    }

    public void deleteAll() {
        // salary_revision goes first: the foreign key cascades, but being explicit keeps this
        // readable and avoids depending on the cascade for correctness.
        jdbc.update("delete from salary_revision");
        jdbc.update("delete from employee");
    }

    /**
     * Takes the ids from the sequence in one round trip, so the revisions can point at their
     * employee without reading anything back.
     */
    private List<Long> reserveIds(int howMany) {
        return jdbc.queryForList(
                "select nextval('employee_id_seq') from generate_series(1, ?)", Long.class, howMany);
    }

    private void insertEmployees(List<Employee> employees, List<Long> ids) {
        jdbc.batchUpdate(INSERT_EMPLOYEE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                bindEmployee(statement, employees.get(index), ids.get(index));
            }

            @Override
            public int getBatchSize() {
                return employees.size();
            }
        });
    }

    private void insertRevisions(List<Employee> employees, List<Long> ids) {
        List<RevisionRow> rows = new ArrayList<>();
        for (int index = 0; index < employees.size(); index++) {
            long employeeId = ids.get(index);
            for (SalaryRevision revision : employees.get(index).getRevisions()) {
                rows.add(new RevisionRow(employeeId, revision));
            }
        }

        jdbc.batchUpdate(INSERT_REVISION, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                RevisionRow row = rows.get(index);
                SalaryRevision revision = row.revision();
                statement.setLong(1, row.employeeId());
                statement.setBigDecimal(2, revision.getSalary().getAmount());
                statement.setString(3, revision.getSalary().getCurrency());
                statement.setDate(4, Date.valueOf(revision.getEffectiveDate()));
                statement.setString(5, revision.getReason().name());
                statement.setTimestamp(6, Timestamp.from(revision.getRecordedAt()));
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    private void bindEmployee(PreparedStatement statement, Employee employee, Long id) throws SQLException {
        statement.setLong(1, id);
        statement.setString(2, employee.getEmployeeCode());
        statement.setString(3, employee.getFirstName());
        statement.setString(4, employee.getLastName());
        statement.setString(5, employee.getEmail());
        statement.setString(6, employee.getCountry().name());
        statement.setString(7, employee.getDepartment().name());
        statement.setString(8, employee.getJobLevel().name());
        statement.setDate(9, Date.valueOf(employee.getHireDate()));
        statement.setDate(10, employee.getExitDate() == null ? null : Date.valueOf(employee.getExitDate()));
    }

    private record RevisionRow(long employeeId, SalaryRevision revision) {
    }
}
