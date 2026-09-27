package com.acme.salary.repository;

import com.acme.salary.dto.EmployeeFilter;
import com.acme.salary.model.Employee;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Turns what the HR Manager typed into a where clause.
 *
 * <p>Built as a specification rather than a fixed query because the filters are independent: any
 * combination of them is valid, and a query method per combination would not be maintainable.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.hasSearchText()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("firstName")), pattern),
                        builder.like(builder.lower(root.get("lastName")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("employeeCode")), pattern)));
            }
            if (filter.country() != null) {
                predicates.add(builder.equal(root.get("country"), filter.country()));
            }
            if (filter.department() != null) {
                predicates.add(builder.equal(root.get("department"), filter.department()));
            }
            if (filter.jobLevel() != null) {
                predicates.add(builder.equal(root.get("jobLevel"), filter.jobLevel()));
            }
            if (!filter.includeLeavers()) {
                // Someone serving notice has an exit date in the future and is still employed today.
                predicates.add(builder.or(
                        builder.isNull(root.get("exitDate")),
                        builder.greaterThanOrEqualTo(root.get("exitDate"), filter.asOf())));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
