package com.acme.salary.service;

import com.acme.salary.config.AnalyticsProperties;
import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.GroupBy;
import com.acme.salary.repository.AnalyticsRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Answers the questions the dashboard asks.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analytics;
    private final AnalyticsProperties properties;
    private final Clock clock;

    public PayrollOverview overview() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<GroupSummary> breakdown(GroupBy groupBy) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<PayOutlier> outliers(int limit) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<ExchangeRateView> exchangeRates() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
