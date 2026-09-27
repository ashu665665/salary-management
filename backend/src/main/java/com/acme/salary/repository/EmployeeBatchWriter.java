package com.acme.salary.repository;

import com.acme.salary.model.Employee;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bulk insert for seeding, bypassing JPA.
 */
@Repository
@RequiredArgsConstructor
public class EmployeeBatchWriter {

    private final JdbcTemplate jdbc;

    public void write(List<Employee> employees) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void deleteAll() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
