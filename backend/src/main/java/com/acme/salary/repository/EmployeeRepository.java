package com.acme.salary.repository;

import com.acme.salary.model.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    /**
     * Loads an employee together with their revisions in one query, for the detail view. The list
     * view deliberately does not use this: fetching a collection alongside a page of rows makes
     * Hibernate page in memory, which does not scale.
     */
    @EntityGraph(attributePaths = "revisions")
    Optional<Employee> findWithRevisionsById(Long id);
}
