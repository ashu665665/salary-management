package com.acme.salary.repository;

import com.acme.salary.dto.EmployeeFilter;
import com.acme.salary.model.Employee;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeFilter filter) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
