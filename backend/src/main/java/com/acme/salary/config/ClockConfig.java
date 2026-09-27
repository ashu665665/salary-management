package com.acme.salary.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Today's date comes from an injected clock rather than {@code LocalDate.now()}, so that "what is
 * in force now" can be tested at a fixed date instead of whatever day the suite happens to run.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
