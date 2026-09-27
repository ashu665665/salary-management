package com.acme.salary.service;

import com.acme.salary.config.SeedProperties;
import com.acme.salary.model.Employee;
import com.acme.salary.repository.EmployeeBatchWriter;
import com.acme.salary.repository.EmployeeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with demo data on startup, when asked to.
 *
 * <p>Seeding is destructive, so the defaults are cautious: it does nothing unless switched on, it
 * refuses to touch a database that already has people in it, and it only deletes when someone
 * explicitly asks for a reset.
 *
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=true"
 * mvn spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=true --app.seed.reset=true"
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final SeedProperties properties;
    private final EmployeeDataFactory factory;
    private final EmployeeBatchWriter writer;
    private final EmployeeRepository employees;
    private final Clock clock;

    public enum SeedOutcome {
        DISABLED,
        ALREADY_POPULATED,
        SEEDED,
        RESET_AND_SEEDED
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public SeedOutcome seed() {
        if (!properties.enabled()) {
            return SeedOutcome.DISABLED;
        }

        boolean alreadyPopulated = employees.count() > 0;
        if (alreadyPopulated && !properties.reset()) {
            log.info("Seeding skipped: the database already has employees. Pass --app.seed.reset=true to replace them.");
            return SeedOutcome.ALREADY_POPULATED;
        }
        if (alreadyPopulated) {
            log.warn("Seeding with reset: deleting every existing employee and salary revision.");
            writer.deleteAll();
        }

        LocalDate today = LocalDate.now(clock);
        long startedAt = System.currentTimeMillis();

        List<Employee> generated = factory.generate(properties.employeeCount(), today, properties.randomSeed());
        writer.write(generated);

        log.info("Seeded {} employees with {} salary revisions in {} ms",
                generated.size(),
                generated.stream().mapToInt(employee -> employee.getRevisions().size()).sum(),
                System.currentTimeMillis() - startedAt);

        return alreadyPopulated ? SeedOutcome.RESET_AND_SEEDED : SeedOutcome.SEEDED;
    }
}
