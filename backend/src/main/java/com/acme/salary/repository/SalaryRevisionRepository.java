package com.acme.salary.repository;

import com.acme.salary.model.SalaryRevision;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRevisionRepository extends JpaRepository<SalaryRevision, Long> {

    /**
     * The salary in force on {@code asOf} for each of the given employees.
     */
    default List<CurrentSalaryRow> findCurrentSalaries(Collection<Long> employeeIds, LocalDate asOf) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
