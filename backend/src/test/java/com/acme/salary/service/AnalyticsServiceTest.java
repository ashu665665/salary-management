package com.acme.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.salary.config.AnalyticsProperties;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.GroupBy;
import com.acme.salary.repository.AnalyticsRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The service decides two things the repository should not: what "now" is, and which rate set the
 * figures are converted at.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private static final AnalyticsProperties PROPERTIES = new AnalyticsProperties("2025-baseline", "USD", 25);

    @Mock
    private AnalyticsRepository repository;

    private AnalyticsService service() {
        return new AnalyticsService(repository, PROPERTIES, CLOCK);
    }

    @Test
    void asks_for_the_figures_as_at_today_using_the_configured_rates() {
        when(repository.overview(TODAY, "2025-baseline")).thenReturn(anOverview());

        service().overview();

        verify(repository).overview(TODAY, "2025-baseline");
    }

    @Test
    void passes_the_grouping_through() {
        when(repository.breakdown(GroupBy.DEPARTMENT, TODAY, "2025-baseline")).thenReturn(List.of());

        service().breakdown(GroupBy.DEPARTMENT);

        verify(repository).breakdown(GroupBy.DEPARTMENT, TODAY, "2025-baseline");
    }

    @Test
    void caps_the_number_of_outliers_at_the_configured_maximum() {
        when(repository.outliers(TODAY, "2025-baseline", 25)).thenReturn(List.of());

        service().outliers(500);

        verify(repository).outliers(TODAY, "2025-baseline", 25);
    }

    @Test
    void honours_a_smaller_outlier_limit() {
        when(repository.outliers(TODAY, "2025-baseline", 5)).thenReturn(List.of());

        service().outliers(5);

        verify(repository).outliers(TODAY, "2025-baseline", 5);
    }

    @Test
    void reports_which_currency_the_figures_are_in() {
        when(repository.overview(TODAY, "2025-baseline")).thenReturn(anOverview());

        PayrollOverview overview = service().overview();

        assertThat(overview.baseCurrency()).isEqualTo("USD");
        assertThat(overview.asOf()).isEqualTo(TODAY);
    }

    private static PayrollOverview anOverview() {
        return new PayrollOverview(TODAY, "USD", 3L,
                new BigDecimal("300000.00"), new BigDecimal("100000.00"), new BigDecimal("6.00"));
    }
}
