package com.acme.salary.service;

import com.acme.salary.dto.EmployeeSummary;
import java.util.List;
import org.springframework.stereotype.Component;

/** Turns a list of employees into a CSV file HR can open in Excel. */
@Component
public class EmployeeCsvWriter {

    public String write(List<EmployeeSummary> employees) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
