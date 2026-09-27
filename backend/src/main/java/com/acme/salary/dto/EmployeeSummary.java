package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import java.time.LocalDate;

/**
 * A row in the employee list. Carries the salary in force on the date the list was asked for,
 * which is why it is not simply the latest revision.
 */
public record EmployeeSummary(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        Country country,
        Department department,
        JobLevel jobLevel,
        LocalDate hireDate,
        LocalDate exitDate,
        boolean active,
        MoneyView currentSalary,
        LocalDate salaryEffectiveFrom) {
}
