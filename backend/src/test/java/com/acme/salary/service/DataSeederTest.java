package com.acme.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.acme.salary.config.SeedProperties;
import com.acme.salary.model.Employee;
import com.acme.salary.repository.EmployeeBatchWriter;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.service.DataSeeder.SeedOutcome;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Seeding is destructive if it gets the decision wrong, so the decision is what these tests cover:
 * when to run, when to stay out of the way, and when clearing is allowed.
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @Mock
    private EmployeeRepository employees;

    @Mock
    private EmployeeBatchWriter writer;

    private DataSeeder seederWith(SeedProperties properties) {
        return new DataSeeder(properties, new EmployeeDataFactory(), writer, employees, CLOCK);
    }

    private static SeedProperties properties(boolean enabled, int count, boolean reset) {
        return new SeedProperties(enabled, count, reset, 20250601L);
    }

    @Test
    void does_nothing_when_seeding_is_switched_off() {
        SeedOutcome outcome = seederWith(properties(false, 50, false)).seed();

        assertThat(outcome).isEqualTo(SeedOutcome.DISABLED);
        verifyNoInteractions(writer, employees);
    }

    @Test
    void seeds_when_the_database_is_empty() {
        when(employees.count()).thenReturn(0L);

        SeedOutcome outcome = seederWith(properties(true, 50, false)).seed();

        assertThat(outcome).isEqualTo(SeedOutcome.SEEDED);
        ArgumentCaptor<List<Employee>> written = ArgumentCaptor.captor();
        verify(writer).write(written.capture());
        assertThat(written.getValue()).hasSize(50);
    }

    @Test
    void leaves_an_existing_database_alone() {
        when(employees.count()).thenReturn(10_000L);

        SeedOutcome outcome = seederWith(properties(true, 50, false)).seed();

        assertThat(outcome).isEqualTo(SeedOutcome.ALREADY_POPULATED);
        verify(writer, never()).write(anyList());
        verify(writer, never()).deleteAll();
    }

    @Test
    void clears_and_reseeds_only_when_reset_is_asked_for() {
        when(employees.count()).thenReturn(10_000L);

        SeedOutcome outcome = seederWith(properties(true, 50, true)).seed();

        assertThat(outcome).isEqualTo(SeedOutcome.RESET_AND_SEEDED);
        verify(writer).deleteAll();
        verify(writer).write(anyList());
    }

    @Test
    void seeds_people_as_at_today() {
        when(employees.count()).thenReturn(0L);

        seederWith(properties(true, 20, false)).seed();

        ArgumentCaptor<List<Employee>> written = ArgumentCaptor.captor();
        verify(writer).write(written.capture());
        assertThat(written.getValue())
                .allSatisfy(employee -> assertThat(employee.getHireDate()).isBeforeOrEqualTo(TODAY));
    }
}
