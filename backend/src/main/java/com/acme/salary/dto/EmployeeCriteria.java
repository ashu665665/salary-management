package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import java.time.LocalDate;

/**
 * What the caller asked for, before it is pinned to a date.
 *
 * <p>Separate from {@link EmployeeFilter} so that the web layer does not have to know what "today"
 * is: it passes on the query, and the service decides the date the answer is as at.
 */
public record EmployeeCriteria(
        String search,
        Country country,
        Department department,
        JobLevel jobLevel,
        boolean includeLeavers) {

    public static EmployeeCriteria unfiltered() {
        return new EmployeeCriteria(null, null, null, null, false);
    }

    public EmployeeFilter asOf(LocalDate date) {
        return new EmployeeFilter(search, country, department, jobLevel, includeLeavers, date);
    }
}
