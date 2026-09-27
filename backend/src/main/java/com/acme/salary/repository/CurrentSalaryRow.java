package com.acme.salary.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The salary in force for one employee on a given date, read straight from the database.
 *
 * <p>A projection rather than the entity: the employee list needs one figure per person, and
 * loading every revision for every row on the page to find it would be wasteful.
 */
public interface CurrentSalaryRow {

    Long getEmployeeId();

    BigDecimal getAmount();

    String getCurrency();

    LocalDate getEffectiveDate();
}
