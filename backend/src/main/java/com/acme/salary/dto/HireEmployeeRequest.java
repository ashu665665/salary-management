package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HireEmployeeRequest(
        @NotBlank @Size(max = 20) String employeeCode,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotNull Country country,
        @NotNull Department department,
        @NotNull JobLevel jobLevel,
        @NotNull LocalDate hireDate,
        @NotNull @Positive BigDecimal salaryAmount,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
