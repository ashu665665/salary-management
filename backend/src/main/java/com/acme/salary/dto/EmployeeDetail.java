package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import java.time.LocalDate;
import java.util.List;

/** One employee with their whole pay history. */
public record EmployeeDetail(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        Country country,
        Department department,
        JobLevel jobLevel,
        LocalDate hireDate,
        LocalDate exitDate,
        boolean active,
        MoneyView currentSalary,
        List<SalaryRevisionView> revisions) {

    public static EmployeeDetail of(Employee employee, LocalDate asOf) {
        return new EmployeeDetail(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.fullName(),
                employee.getEmail(),
                employee.getCountry(),
                employee.getDepartment(),
                employee.getJobLevel(),
                employee.getHireDate(),
                employee.getExitDate(),
                employee.isActiveOn(asOf),
                employee.salaryOn(asOf).map(MoneyView::of).orElse(null),
                employee.getRevisions().stream().map(SalaryRevisionView::of).toList());
    }
}
