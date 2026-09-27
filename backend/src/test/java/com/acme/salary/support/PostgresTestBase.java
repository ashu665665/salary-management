package com.acme.salary.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A real Postgres for tests that touch the database.
 *
 * <p>The tests run against the same engine and the same Flyway migrations as production, so a
 * mapping or SQL mistake fails here rather than at startup. {@code ddl-auto: validate} stays on, so
 * an entity that drifts from the schema breaks the build.
 *
 * <p>The container is started once and shared by every test class that extends this, instead of
 * using {@code @Testcontainers}, which would start a fresh one per class and make the suite slow.
 * Docker stops it when the JVM exits.
 */
public abstract class PostgresTestBase {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
