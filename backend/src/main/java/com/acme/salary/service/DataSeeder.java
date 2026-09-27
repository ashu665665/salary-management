package com.acme.salary.service;

import com.acme.salary.config.SeedProperties;
import com.acme.salary.repository.EmployeeBatchWriter;
import com.acme.salary.repository.EmployeeRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fills an empty database with demo data on startup, when asked to.
 */
@Component
@RequiredArgsConstructor
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

    public SeedOutcome seed() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
