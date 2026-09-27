package com.acme.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How reported figures are produced.
 *
 * @param rateSet        which set of exchange rates converts local pay into the base currency
 * @param baseCurrency   the currency every reported total is expressed in
 * @param maximumOutliers an upper bound on the outlier list, so one request cannot ask for 10,000
 */
@ConfigurationProperties(prefix = "app.analytics")
public record AnalyticsProperties(
        @DefaultValue("2025-baseline") String rateSet,
        @DefaultValue("USD") String baseCurrency,
        @DefaultValue("25") int maximumOutliers) {
}
