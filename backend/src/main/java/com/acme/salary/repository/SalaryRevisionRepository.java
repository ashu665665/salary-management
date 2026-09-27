package com.acme.salary.repository;

import com.acme.salary.model.SalaryRevision;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalaryRevisionRepository extends JpaRepository<SalaryRevision, Long> {

    /**
     * The salary in force on {@code asOf} for each of the given employees, as one query.
     *
     * <p>Written in SQL because Postgres has exactly the right tool: {@code distinct on} keeps the
     * first row per employee, and the ordering makes that the newest revision not after the date.
     * The alternative, loading every revision for every employee on the page and picking in Java,
     * is the N+1 problem with extra steps.
     *
     * <p>Callers must not pass an empty collection: {@code in ()} is not valid SQL.
     */
    @Query(value = """
            select distinct on (r.employee_id)
                   r.employee_id    as employeeId,
                   r.amount         as amount,
                   r.currency       as currency,
                   r.effective_date as effectiveDate
            from salary_revision r
            where r.employee_id in (:employeeIds)
              and r.effective_date <= :asOf
            order by r.employee_id, r.effective_date desc
            """, nativeQuery = true)
    List<CurrentSalaryRow> findCurrentSalaries(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("asOf") LocalDate asOf);
}
