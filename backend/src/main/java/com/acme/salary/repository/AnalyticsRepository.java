package com.acme.salary.repository;

import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.GroupBy;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The reporting queries.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PayrollOverview overview(LocalDate asOf, String rateSet) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<GroupSummary> breakdown(GroupBy groupBy, LocalDate asOf, String rateSet) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<PayOutlier> outliers(LocalDate asOf, String rateSet, int limit) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<ExchangeRateView> exchangeRates(String rateSet) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
