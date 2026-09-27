package com.acme.salary.model;

/**
 * The dimensions the payroll can be broken down by.
 *
 * <p>An enum rather than a free-text field: the value reaches a group-by clause, so the set of
 * allowed columns has to be closed. Anything outside this list is rejected before it gets near SQL.
 */
public enum GroupBy {

    COUNTRY("country"),
    DEPARTMENT("department"),
    JOB_LEVEL("job_level");

    private final String column;

    GroupBy(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}
