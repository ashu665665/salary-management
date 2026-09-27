package com.acme.salary.repository;

import com.acme.salary.dto.ExchangeRateView;
import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.GroupBy;
import com.acme.salary.model.JobLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The reporting queries, written in SQL on purpose.
 *
 * <p>Medians and quartiles over 10,000 people are the database's job. Loading every salary into
 * the application to sort it there would move a lot of data to compute one number, and Postgres
 * has {@code percentile_cont} built for exactly this.
 *
 * <p>Every figure is produced as at a date and converted through one named rate set, so the same
 * question asked twice gives the same answer.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    /**
     * Everyone employed on the date, with the salary then in force converted to the base currency.
     *
     * <p>Shared by every query below so that "who counts" and "what they earn" are defined once.
     */
    private static final String EMPLOYED_AND_PAID = """
            with current_salary as (
                select distinct on (r.employee_id)
                       r.employee_id, r.amount, r.currency
                from salary_revision r
                where r.effective_date <= :asOf
                order by r.employee_id, r.effective_date desc
            ),
            paid as (
                select e.id,
                       e.employee_code,
                       e.first_name,
                       e.last_name,
                       e.country,
                       e.department,
                       e.job_level,
                       round(cs.amount / fx.units_per_usd, 2) as amount_base
                from employee e
                join current_salary cs on cs.employee_id = e.id
                join exchange_rate fx on fx.currency = cs.currency and fx.rate_set = :rateSet
                where e.exit_date is null or e.exit_date >= :asOf
            )
            """;

    /**
     * A peer group of two says nothing useful about a range, so small groups are not reported on.
     */
    private static final int SMALLEST_MEANINGFUL_PEER_GROUP = 5;

    private final NamedParameterJdbcTemplate jdbc;

    public PayrollOverview overview(LocalDate asOf, String rateSet) {
        MapSqlParameterSource parameters = asOfAndRates(asOf, rateSet);

        PayrollOverview totals = jdbc.queryForObject(EMPLOYED_AND_PAID + """
                select count(*)                                                              as headcount,
                       coalesce(sum(amount_base), 0)                                         as total_cost,
                       coalesce(round(percentile_cont(0.5)
                           within group (order by amount_base)::numeric, 2), 0)              as median_salary
                from paid
                """, parameters, (rs, row) -> new PayrollOverview(
                        asOf,
                        null,
                        rs.getLong("headcount"),
                        rs.getBigDecimal("total_cost"),
                        rs.getBigDecimal("median_salary"),
                        null));

        return new PayrollOverview(
                asOf,
                null,
                totals.headcount(),
                totals.totalAnnualCost(),
                totals.medianSalary(),
                averageIncreasePercent(asOf));
    }

    /**
     * How much pay has moved in a year, averaged over the people who were here for all of it.
     *
     * <p>Compared in the employee's own currency: a percentage does not need converting, and using
     * the base currency would fold exchange-rate movement into what should measure pay decisions.
     * Anyone who changed currency is left out for the same reason.
     */
    private BigDecimal averageIncreasePercent(LocalDate asOf) {
        BigDecimal average = jdbc.queryForObject("""
                with salary_now as (
                    select distinct on (r.employee_id) r.employee_id, r.amount, r.currency
                    from salary_revision r
                    where r.effective_date <= :asOf
                    order by r.employee_id, r.effective_date desc
                ),
                salary_a_year_ago as (
                    select distinct on (r.employee_id) r.employee_id, r.amount, r.currency
                    from salary_revision r
                    where r.effective_date <= cast(:asOf as date) - interval '1 year'
                    order by r.employee_id, r.effective_date desc
                )
                select round(avg((now.amount - before.amount) / before.amount * 100)::numeric, 2)
                from employee e
                join salary_now now on now.employee_id = e.id
                join salary_a_year_ago before
                     on before.employee_id = e.id and before.currency = now.currency
                where (e.exit_date is null or e.exit_date >= :asOf)
                  and before.amount > 0
                """, new MapSqlParameterSource("asOf", asOf), BigDecimal.class);

        return average == null ? BigDecimal.ZERO : average;
    }

    public List<GroupSummary> breakdown(GroupBy groupBy, LocalDate asOf, String rateSet) {
        // Safe to interpolate: GroupBy is an enum, so the column can only be one of three literals.
        String column = groupBy.getColumn();

        return jdbc.query(EMPLOYED_AND_PAID + """
                select %s                                                          as group_key,
                       count(*)                                                    as headcount,
                       sum(amount_base)                                            as total_cost,
                       round(percentile_cont(0.50)
                           within group (order by amount_base)::numeric, 2)        as median_salary,
                       round(percentile_cont(0.25)
                           within group (order by amount_base)::numeric, 2)        as lower_quartile,
                       round(percentile_cont(0.75)
                           within group (order by amount_base)::numeric, 2)        as upper_quartile
                from paid
                group by %s
                order by total_cost desc
                """.formatted(column, column),
                asOfAndRates(asOf, rateSet),
                (rs, row) -> new GroupSummary(
                        rs.getString("group_key"),
                        rs.getLong("headcount"),
                        rs.getBigDecimal("total_cost"),
                        rs.getBigDecimal("median_salary"),
                        rs.getBigDecimal("lower_quartile"),
                        rs.getBigDecimal("upper_quartile")));
    }

    /**
     * People paid outside the middle half of their peer group, worst first.
     *
     * <p>Peers are same country and same level: comparing an Indian junior with an American one
     * would flag the entire Indian office, which is a fact about currencies, not about pay
     * decisions.
     */
    public List<PayOutlier> outliers(LocalDate asOf, String rateSet, int limit) {
        MapSqlParameterSource parameters = asOfAndRates(asOf, rateSet)
                .addValue("minimumPeers", SMALLEST_MEANINGFUL_PEER_GROUP)
                .addValue("limit", limit);

        return jdbc.query(EMPLOYED_AND_PAID + """
                , peer_group as (
                    select country,
                           job_level,
                           count(*)                                                    as peers,
                           percentile_cont(0.50) within group (order by amount_base)    as median_salary,
                           percentile_cont(0.25) within group (order by amount_base)    as lower_quartile,
                           percentile_cont(0.75) within group (order by amount_base)    as upper_quartile
                    from paid
                    group by country, job_level
                )
                select p.id,
                       p.employee_code,
                       p.first_name,
                       p.last_name,
                       p.country,
                       p.department,
                       p.job_level,
                       p.amount_base,
                       round(g.median_salary::numeric, 2)   as median_salary,
                       round(g.lower_quartile::numeric, 2)  as lower_quartile,
                       round(g.upper_quartile::numeric, 2)  as upper_quartile,
                       case when p.amount_base < g.lower_quartile then 'BELOW_RANGE'
                            else 'ABOVE_RANGE' end          as position
                from paid p
                join peer_group g on g.country = p.country and g.job_level = p.job_level
                where g.peers >= :minimumPeers
                  and (p.amount_base < g.lower_quartile or p.amount_base > g.upper_quartile)
                -- How far outside the range, measured in quartile widths, the way a boxplot does
                -- it. Dividing by the quartile instead would flatter the overpaid: a salary can
                -- sit three times above the upper quartile but never more than once below the
                -- lower one, which would push the underpaid off a list that exists to find them.
                order by case when p.amount_base < g.lower_quartile
                              then (g.lower_quartile - p.amount_base)
                              else (p.amount_base - g.upper_quartile)
                         end / nullif(g.upper_quartile - g.lower_quartile, 0) desc
                limit :limit
                """, parameters, (rs, row) -> new PayOutlier(
                        rs.getLong("id"),
                        rs.getString("employee_code"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        Country.valueOf(rs.getString("country")),
                        Department.valueOf(rs.getString("department")),
                        JobLevel.valueOf(rs.getString("job_level")),
                        rs.getBigDecimal("amount_base"),
                        rs.getBigDecimal("median_salary"),
                        rs.getBigDecimal("lower_quartile"),
                        rs.getBigDecimal("upper_quartile"),
                        rs.getString("position")));
    }

    public List<ExchangeRateView> exchangeRates(String rateSet) {
        return jdbc.query(
                "select currency, units_per_usd from exchange_rate where rate_set = :rateSet order by currency",
                Map.of("rateSet", rateSet),
                (rs, row) -> new ExchangeRateView(rs.getString("currency"), rs.getBigDecimal("units_per_usd")));
    }

    private MapSqlParameterSource asOfAndRates(LocalDate asOf, String rateSet) {
        return new MapSqlParameterSource()
                .addValue("asOf", asOf)
                .addValue("rateSet", rateSet);
    }
}
