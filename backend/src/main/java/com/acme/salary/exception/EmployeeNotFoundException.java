package com.acme.salary.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("No employee with id " + id);
    }
}
