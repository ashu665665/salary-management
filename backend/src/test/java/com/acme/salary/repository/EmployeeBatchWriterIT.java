package com.acme.salary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import com.acme.salary.support.PostgresTestBase;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class EmployeeBatchWriterIT extends PostgresTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private EmployeeBatchWriter writer;

    @BeforeEach
    void setUp() {
        writer = new EmployeeBatchWriter(jdbc);
        jdbc.execute("delete from salary_revision");
        jdbc.execute("delete from employee");
    }

    private static Employee anEmployee(int number, Country country) {
        Employee employee = Employee.hire(
                "ACME-%05d".formatted(number),
                "First" + number,
                "Last" + number,
                "person%d@acme.example".formatted(number),
                country,
                Department.ENGINEERING,
                JobLevel.MID,
                LocalDate.of(2021, 3, 1),
                Money.of("100000", country.getPayCurrency()));
        employee.recordRevision(
                Money.of("110000", country.getPayCurrency()),
                LocalDate.of(2023, 4, 1),
                RevisionReason.ANNUAL_RAISE);
        return employee;
    }

    @Test
    void writes_employees_and_their_revisions() {
        writer.write(List.of(anEmployee(1, Country.UNITED_STATES), anEmployee(2, Country.INDIA)));

        assertThat(count("employee")).isEqualTo(2);
        assertThat(count("salary_revision")).isEqualTo(4);
    }

    @Test
    void points_every_revision_at_the_right_employee() {
        writer.write(List.of(anEmployee(1, Country.UNITED_STATES), anEmployee(2, Country.INDIA)));

        List<Map<String, Object>> rows = jdbc.queryForList("""
                select e.employee_code as code, r.currency as currency, count(*) as revisions
                from employee e join salary_revision r on r.employee_id = e.id
                group by e.employee_code, r.currency
                order by e.employee_code
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("code", "ACME-00001").containsEntry("currency", "USD");
        assertThat(rows.get(1)).containsEntry("code", "ACME-00002").containsEntry("currency", "INR");
        assertThat(rows).allSatisfy(row -> assertThat(row.get("revisions")).isEqualTo(2L));
    }

    @Test
    void stores_the_amount_the_currency_and_the_reason() {
        writer.write(List.of(anEmployee(1, Country.GERMANY)));

        Map<String, Object> hire = jdbc.queryForMap(
                "select amount, currency, reason, effective_date from salary_revision order by effective_date limit 1");

        assertThat(hire).containsEntry("currency", "EUR").containsEntry("reason", "HIRE");
        assertThat((java.math.BigDecimal) hire.get("amount")).isEqualByComparingTo("100000");
    }

    @Test
    void writes_a_large_batch_in_one_call() {
        List<Employee> many = java.util.stream.IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> anEmployee(i, Country.POLAND))
                .toList();

        writer.write(many);

        assertThat(count("employee")).isEqualTo(1000);
        assertThat(count("salary_revision")).isEqualTo(2000);
    }

    @Test
    void clears_everything_when_asked() {
        writer.write(List.of(anEmployee(1, Country.BRAZIL)));

        writer.deleteAll();

        assertThat(count("employee")).isZero();
        assertThat(count("salary_revision")).isZero();
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }
}
