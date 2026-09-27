package com.acme.salary.service;

import com.acme.salary.dto.EmployeeSummary;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Turns a list of employees into a CSV file HR can open in Excel.
 *
 * <p>Written by hand rather than with a library: it is one well-defined format, the escaping rules
 * fit in a few lines, and the tests pin them down. A dependency would be the larger thing to carry.
 *
 * <p>Two details exist purely because the file is opened in Excel. Rows end with CRLF, and the file
 * starts with a byte order mark, without which Excel reads a UTF-8 name like "Wójcik" as mojibake.
 */
@Component
public class EmployeeCsvWriter {

    private static final String BYTE_ORDER_MARK = "﻿";
    private static final String ROW_SEPARATOR = "\r\n";
    private static final char SEPARATOR = ',';

    private static final List<String> HEADERS = List.of(
            "Employee Code", "Name", "Email", "Country", "Department", "Level",
            "Hire Date", "Exit Date", "Status", "Salary", "Currency", "Salary Effective From");

    public String write(List<EmployeeSummary> employees) {
        StringBuilder csv = new StringBuilder(BYTE_ORDER_MARK);
        appendRow(csv, HEADERS);
        for (EmployeeSummary employee : employees) {
            appendRow(csv, toFields(employee));
        }
        return csv.toString();
    }

    private static List<String> toFields(EmployeeSummary employee) {
        return List.of(
                text(employee.employeeCode()),
                text(employee.fullName()),
                text(employee.email()),
                text(employee.country()),
                text(employee.department()),
                text(employee.jobLevel()),
                text(employee.hireDate()),
                text(employee.exitDate()),
                employee.active() ? "Active" : "Left",
                // Unformatted, so Excel sees a number rather than a string it cannot total.
                employee.currentSalary() == null ? "" : employee.currentSalary().amount().toPlainString(),
                employee.currentSalary() == null ? "" : employee.currentSalary().currency(),
                text(employee.salaryEffectiveFrom()));
    }

    private static void appendRow(StringBuilder csv, List<String> fields) {
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(escape(fields.get(index)));
        }
        csv.append(ROW_SEPARATOR);
    }

    /**
     * A field only needs quoting when it contains something that would otherwise end the field or
     * the row. Inside quotes, a quote is written twice.
     */
    private static String escape(String field) {
        boolean needsQuoting = field.indexOf(SEPARATOR) >= 0
                || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0
                || field.indexOf('\r') >= 0;

        return needsQuoting ? '"' + field.replace("\"", "\"\"") + '"' : field;
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }
}
