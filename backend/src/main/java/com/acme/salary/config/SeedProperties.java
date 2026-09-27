package com.acme.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the demo data is generated.
 *
 * <p>Off by default: seeding writes 10,000 people and, with {@code reset}, deletes what is already
 * there. Nothing that destructive should happen because someone started the app.
 *
 * @param randomSeed fixing this makes the generated organisation the same every run
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10000") int employeeCount,
        @DefaultValue("false") boolean reset,
        @DefaultValue("20250601") long randomSeed) {
}
