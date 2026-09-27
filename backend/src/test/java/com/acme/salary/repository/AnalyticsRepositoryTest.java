package com.acme.salary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.dto.GroupSummary;
import com.acme.salary.dto.PayOutlier;
import com.acme.salary.dto.PayrollOverview;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.GroupBy;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.support.PostgresTestBase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * The analytics are the reason this system exists rather than a spreadsheet, so the figures are
 * checked against numbers worked out by hand rather than against whatever the query happens to
 * return.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AnalyticsRepositoryTest extends PostgresTestBase {

    private static final LocalDate AS_OF = LocalDate.of(2025, 6, 1);
    private static final LocalDate HIRED = LocalDate.of(2020, 1, 1);
    private static final String RATE_SET = "2025-baseline";

    @Autowired
    private JdbcTemplate jdbc;

    private AnalyticsRepository analytics;
    private EmployeeBatchWriter writer;
    private final List<Employee> people = new ArrayList<>();

    @BeforeEach
    void setUp() {
        analytics = new AnalyticsRepository(new NamedParameterJdbcTemplate(jdbc));
        writer = new EmployeeBatchWriter(jdbc);
        jdbc.execute("delete from salary_revision");
        jdbc.execute("delete from employee");
        people.clear();
    }

    /** Salaries are chosen so every expected figure below can be worked out in your head. */
    private Employee person(String code, Country country, JobLevel level, String salary) {
        Employee employee = Employee.hire(
                code, "First" + code, "Last" + code, code.toLowerCase() + "@acme.example",
                country, Department.ENGINEERING, level, HIRED, Money.of(salary, country.getPayCurrency()));
        people.add(employee);
        return employee;
    }

    private void save() {
        writer.write(people);
    }

    /**
     * Four Americans on 100k, 200k, 300k and 400k, and two Indians on the rupee equivalent of
     * 100k and 50k. In USD that is 50k, 100k, 100k, 200k, 300k, 400k.
     */
    private void anOrganisation() {
        person("US-1", Country.UNITED_STATES, JobLevel.JUNIOR, "100000");
        person("US-2", Country.UNITED_STATES, JobLevel.MID, "200000");
        person("US-3", Country.UNITED_STATES, JobLevel.SENIOR, "300000");
        person("US-4", Country.UNITED_STATES, JobLevel.LEAD, "400000");
        person("IN-1", Country.INDIA, JobLevel.MID, "8300000");
        person("IN-2", Country.INDIA, JobLevel.JUNIOR, "4150000");
        save();
    }

    @Nested
    class Overview {

        @Test
        void counts_everyone_and_totals_their_pay_in_the_base_currency() {
            anOrganisation();

            PayrollOverview overview = analytics.overview(AS_OF, RATE_SET);

            assertThat(overview.headcount()).isEqualTo(6);
            assertThat(overview.totalAnnualCost()).isEqualByComparingTo("1150000.00");
        }

        @Test
        void reports_the_median_rather_than_the_average() {
            anOrganisation();

            // Sorted: 50k, 100k, 100k, 200k, 300k, 400k. Median is halfway between 100k and 200k.
            // The average would be 191,666, dragged up by the highest earner.
            assertThat(analytics.overview(AS_OF, RATE_SET).medianSalary()).isEqualByComparingTo("150000.00");
        }

        @Test
        void converts_each_country_at_its_own_rate() {
            person("IN-1", Country.INDIA, JobLevel.MID, "8300000");
            save();

            assertThat(analytics.overview(AS_OF, RATE_SET).totalAnnualCost()).isEqualByComparingTo("100000.00");
        }

        @Test
        void leaves_out_people_who_have_already_left() {
            anOrganisation();
            Employee leaver = person("US-9", Country.UNITED_STATES, JobLevel.MANAGER, "900000");
            leaver.markExit(AS_OF.minusDays(1));
            writer.write(List.of(leaver));

            assertThat(analytics.overview(AS_OF, RATE_SET).headcount()).isEqualTo(6);
        }

        @Test
        void counts_someone_whose_last_day_has_not_arrived_yet() {
            Employee leaving = person("US-8", Country.UNITED_STATES, JobLevel.MANAGER, "500000");
            leaving.markExit(AS_OF.plusMonths(2));
            save();

            assertThat(analytics.overview(AS_OF, RATE_SET).headcount()).isEqualTo(1);
        }

        @Test
        void uses_the_salary_in_force_on_the_date_asked_for() {
            Employee employee = person("US-1", Country.UNITED_STATES, JobLevel.MID, "100000");
            employee.recordRevision(Money.of("150000", "USD"), AS_OF.plusMonths(1), RevisionReason.ANNUAL_RAISE);
            save();

            assertThat(analytics.overview(AS_OF, RATE_SET).totalAnnualCost()).isEqualByComparingTo("100000.00");
        }

        @Test
        void answers_with_zeroes_when_there_is_nobody() {
            PayrollOverview overview = analytics.overview(AS_OF, RATE_SET);

            assertThat(overview.headcount()).isZero();
            assertThat(overview.totalAnnualCost()).isEqualByComparingTo("0");
            assertThat(overview.medianSalary()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class YearOnYear {

        @Test
        void averages_the_increase_against_what_people_earned_a_year_ago() {
            Employee tenPercent = person("US-1", Country.UNITED_STATES, JobLevel.MID, "100000");
            tenPercent.recordRevision(Money.of("110000", "USD"), AS_OF.minusMonths(2), RevisionReason.ANNUAL_RAISE);
            Employee twentyPercent = person("US-2", Country.UNITED_STATES, JobLevel.MID, "100000");
            twentyPercent.recordRevision(Money.of("120000", "USD"), AS_OF.minusMonths(3), RevisionReason.ANNUAL_RAISE);
            save();

            assertThat(analytics.overview(AS_OF, RATE_SET).averageIncreasePercent())
                    .isEqualByComparingTo("15.00");
        }

        @Test
        void counts_someone_with_no_raise_as_no_increase() {
            person("US-1", Country.UNITED_STATES, JobLevel.MID, "100000");
            Employee raised = person("US-2", Country.UNITED_STATES, JobLevel.MID, "100000");
            raised.recordRevision(Money.of("120000", "USD"), AS_OF.minusMonths(3), RevisionReason.ANNUAL_RAISE);
            save();

            assertThat(analytics.overview(AS_OF, RATE_SET).averageIncreasePercent())
                    .isEqualByComparingTo("10.00");
        }

        @Test
        void ignores_people_who_were_not_here_a_year_ago() {
            Employee newJoiner = person("US-1", Country.UNITED_STATES, JobLevel.MID, "100000");
            newJoiner.correctEmail("new@acme.example");
            Employee established = person("US-2", Country.UNITED_STATES, JobLevel.MID, "100000");
            established.recordRevision(Money.of("110000", "USD"), AS_OF.minusMonths(2), RevisionReason.ANNUAL_RAISE);
            writer.write(List.of(established));

            assertThat(analytics.overview(AS_OF, RATE_SET).averageIncreasePercent())
                    .isEqualByComparingTo("10.00");
        }
    }

    @Nested
    class Breakdown {

        @Test
        void splits_headcount_and_cost_by_country() {
            anOrganisation();

            List<GroupSummary> rows = analytics.breakdown(GroupBy.COUNTRY, AS_OF, RATE_SET);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).group()).isEqualTo("UNITED_STATES");
            assertThat(rows.get(0).headcount()).isEqualTo(4);
            assertThat(rows.get(0).totalAnnualCost()).isEqualByComparingTo("1000000.00");
            assertThat(rows.get(1).group()).isEqualTo("INDIA");
            assertThat(rows.get(1).totalAnnualCost()).isEqualByComparingTo("150000.00");
        }

        @Test
        void reports_quartiles_for_each_group() {
            anOrganisation();

            GroupSummary unitedStates = analytics.breakdown(GroupBy.COUNTRY, AS_OF, RATE_SET).get(0);

            // 100k, 200k, 300k, 400k: median 250k, lower quartile 175k, upper quartile 325k.
            assertThat(unitedStates.medianSalary()).isEqualByComparingTo("250000.00");
            assertThat(unitedStates.lowerQuartile()).isEqualByComparingTo("175000.00");
            assertThat(unitedStates.upperQuartile()).isEqualByComparingTo("325000.00");
        }

        @Test
        void splits_by_department() {
            anOrganisation();

            List<GroupSummary> rows = analytics.breakdown(GroupBy.DEPARTMENT, AS_OF, RATE_SET);

            assertThat(rows).singleElement()
                    .satisfies(row -> assertThat(row.group()).isEqualTo("ENGINEERING"));
        }

        @Test
        void splits_by_level() {
            anOrganisation();

            List<GroupSummary> rows = analytics.breakdown(GroupBy.JOB_LEVEL, AS_OF, RATE_SET);

            assertThat(rows).extracting(GroupSummary::group)
                    .containsExactlyInAnyOrder("JUNIOR", "MID", "SENIOR", "LEAD");
        }

        @Test
        void orders_the_most_expensive_group_first() {
            anOrganisation();

            assertThat(analytics.breakdown(GroupBy.COUNTRY, AS_OF, RATE_SET))
                    .extracting(GroupSummary::totalAnnualCost)
                    .isSortedAccordingTo((first, second) -> second.compareTo(first));
        }
    }

    @Nested
    class Outliers {

        /** Eight mid-level Americans, seven of them clustered and one paid far less. */
        private void aPeerGroupWithOneUnderpaidPerson() {
            for (int i = 1; i <= 7; i++) {
                person("US-" + i, Country.UNITED_STATES, JobLevel.MID, "100000");
            }
            person("US-LOW", Country.UNITED_STATES, JobLevel.MID, "40000");
            save();
        }

        @Test
        void finds_someone_paid_below_their_peer_group() {
            aPeerGroupWithOneUnderpaidPerson();

            List<PayOutlier> outliers = analytics.outliers(AS_OF, RATE_SET, 10);

            assertThat(outliers).singleElement().satisfies(outlier -> {
                assertThat(outlier.employeeCode()).isEqualTo("US-LOW");
                assertThat(outlier.position()).isEqualTo("BELOW_RANGE");
                assertThat(outlier.salary()).isEqualByComparingTo("40000.00");
            });
        }

        @Test
        void finds_someone_paid_above_their_peer_group() {
            for (int i = 1; i <= 7; i++) {
                person("US-" + i, Country.UNITED_STATES, JobLevel.MID, "100000");
            }
            person("US-HIGH", Country.UNITED_STATES, JobLevel.MID, "300000");
            save();

            assertThat(analytics.outliers(AS_OF, RATE_SET, 10))
                    .singleElement()
                    .satisfies(outlier -> assertThat(outlier.position()).isEqualTo("ABOVE_RANGE"));
        }

        @Test
        void compares_people_only_against_their_own_country_and_level() {
            aPeerGroupWithOneUnderpaidPerson();
            // An Indian junior earns far less in USD but is not an outlier: nobody to compare with.
            person("IN-1", Country.INDIA, JobLevel.JUNIOR, "1000000");
            writer.write(List.of(people.get(people.size() - 1)));

            assertThat(analytics.outliers(AS_OF, RATE_SET, 10))
                    .extracting(PayOutlier::employeeCode)
                    .containsExactly("US-LOW");
        }

        @Test
        void ignores_peer_groups_too_small_to_say_anything() {
            person("US-1", Country.UNITED_STATES, JobLevel.MID, "100000");
            person("US-2", Country.UNITED_STATES, JobLevel.MID, "500000");
            save();

            assertThat(analytics.outliers(AS_OF, RATE_SET, 10)).isEmpty();
        }

        @Test
        void returns_the_peer_range_alongside_the_person() {
            aPeerGroupWithOneUnderpaidPerson();

            PayOutlier outlier = analytics.outliers(AS_OF, RATE_SET, 10).get(0);

            assertThat(outlier.peerMedian()).isEqualByComparingTo("100000.00");
            assertThat(outlier.lowerQuartile()).isPositive();
            assertThat(outlier.upperQuartile()).isEqualByComparingTo("100000.00");
            assertThat(outlier.fullName()).isNotBlank();
        }

        @Test
        void returns_no_more_than_the_limit_asked_for() {
            for (int i = 1; i <= 6; i++) {
                person("US-" + i, Country.UNITED_STATES, JobLevel.MID, "100000");
            }
            person("US-LOW-1", Country.UNITED_STATES, JobLevel.MID, "30000");
            person("US-LOW-2", Country.UNITED_STATES, JobLevel.MID, "40000");
            save();

            assertThat(analytics.outliers(AS_OF, RATE_SET, 1)).hasSize(1);
        }
    }

    @Nested
    class Rates {

        @Test
        void ships_a_rate_for_every_currency_the_company_pays_in() {
            List<String> currencies = analytics.exchangeRates(RATE_SET).stream()
                    .map(rate -> rate.currency())
                    .toList();

            assertThat(currencies).contains(
                    Country.INDIA.getPayCurrency(),
                    Country.JAPAN.getPayCurrency(),
                    Country.BRAZIL.getPayCurrency(),
                    Country.UNITED_STATES.getPayCurrency());
        }
    }
}
