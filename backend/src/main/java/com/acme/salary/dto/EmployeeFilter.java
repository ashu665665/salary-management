package com.acme.salary.dto;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import java.time.LocalDate;

/**
 * What the HR Manager is currently looking at: free text plus the usual narrowing-down, as at a
 * date. The date matters because who counts as an employee depends on when you ask.
 *
 * @param search       matched against name, employee code and email; null or blank means no filter
 * @param includeLeavers whether people who have left should appear
 * @param asOf         the date the list describes
 */
public record EmployeeFilter(
        String search,
        Country country,
        Department department,
        JobLevel jobLevel,
        boolean includeLeavers,
        LocalDate asOf) {

    public static EmployeeFilter activeOn(LocalDate asOf) {
        return new EmployeeFilter(null, null, null, null, false, asOf);
    }

    public boolean hasSearchText() {
        return search != null && !search.isBlank();
    }
}
