package com.acme.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the demo data is generated.
 *
 * <p>A small organisation by default, so a fresh database has something to show without carrying
 * data nobody asked for. The generator is not limited to that size: point
 * {@code app.seed.employee-count} at 10,000 and it produces a full-sized organisation.
 *
 * <p>Seeding only ever fills an empty database. Replacing data that is already there needs
 * {@code reset}, which stays off, because deleting every salary record should never be something
 * that happens merely because the application started.
 *
 * @param randomSeed fixing this makes the generated organisation the same every run
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("25") int employeeCount,
        @DefaultValue("false") boolean reset,
        @DefaultValue("20250601") long randomSeed) {
}
