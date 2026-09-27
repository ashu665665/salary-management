package com.acme.salary.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The seeding defaults decide what happens to a database nobody configured, so they are worth
 * pinning down rather than leaving to whatever the record happens to say.
 */
class SeedPropertiesTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(EnableSeedProperties.class);

    @Test
    void seeds_a_small_demo_organisation_by_default() {
        context.run(loaded -> {
            SeedProperties properties = loaded.getBean(SeedProperties.class);

            assertThat(properties.enabled()).isTrue();
            assertThat(properties.employeeCount()).isEqualTo(25);
        });
    }

    @Test
    void scales_up_to_a_full_sized_organisation_when_asked() {
        context.withPropertyValues("app.seed.employee-count=10000").run(loaded ->
                assertThat(loaded.getBean(SeedProperties.class).employeeCount()).isEqualTo(10_000));
    }

    @Test
    void never_clears_an_existing_database_unless_told_to() {
        context.run(loaded -> assertThat(loaded.getBean(SeedProperties.class).reset()).isFalse());
    }

    @Test
    void produces_the_same_organisation_every_time_unless_the_seed_is_changed() {
        context.run(loaded -> assertThat(loaded.getBean(SeedProperties.class).randomSeed()).isEqualTo(20250601L));
    }

    @EnableConfigurationProperties(SeedProperties.class)
    static class EnableSeedProperties {
    }
}
