package com.acme.salary.service;

import com.acme.salary.model.Employee;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds a plausible organisation of a given size.
 */
@Component
public class EmployeeDataFactory {

    public List<Employee> generate(int count, LocalDate asOf, long randomSeed) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
