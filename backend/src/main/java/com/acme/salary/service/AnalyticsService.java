package com.acme.salary.service;

import com.acme.salary.config.AnalyticsProperties;
import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.GroupBy;
import com.acme.salary.repository.AnalyticsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers the questions the dashboard asks.
 *
 * <p>It decides the two things the queries should not decide for themselves: what "now" means, and
 * which set of exchange rates the figures are converted through.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analytics;
    private final AnalyticsProperties properties;
    private final Clock clock;

    public PayrollOverview overview() {
        PayrollOverview overview = analytics.overview(today(), properties.rateSet());
        return new PayrollOverview(
                overview.asOf(),
                properties.baseCurrency(),
                overview.headcount(),
                overview.totalAnnualCost(),
                overview.medianSalary(),
                overview.averageIncreasePercent());
    }

    public List<GroupSummary> breakdown(GroupBy groupBy) {
        return analytics.breakdown(groupBy, today(), properties.rateSet());
    }

    /**
     * The worst-paid-relative-to-peers first, never more than the configured maximum however many
     * the caller asks for.
     */
    public List<PayOutlier> outliers(int limit) {
        return analytics.outliers(today(), properties.rateSet(), Math.min(limit, properties.maximumOutliers()));
    }

    public List<ExchangeRateView> exchangeRates() {
        return analytics.exchangeRates(properties.rateSet());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
