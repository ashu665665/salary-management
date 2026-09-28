package com.acme.salary.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void takes_the_scale_of_its_currency() {
        assertThat(Money.of("1200", "USD").getAmount()).isEqualByComparingTo("1200.00");
        assertThat(Money.of("1200", "USD").toString()).isEqualTo("1200.00 USD");
    }

    @Test
    void has_no_decimals_for_a_currency_that_has_none() {
        // Japanese salaries are quoted in whole yen.
        assertThat(Money.of("5400000.4", "JPY").toString()).isEqualTo("5400000 JPY");
    }

    @Test
    void rounds_half_up_to_the_currency_scale() {
        assertThat(Money.of("100.005", "EUR").getAmount()).isEqualByComparingTo("100.01");
    }

    @Test
    void rejects_an_unknown_currency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of("100", "XYZ"))
                .withMessageContaining("unknown currency code");
    }

    @Test
    void rejects_a_negative_amount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of("-1", "INR"))
                .withMessageContaining("cannot be negative");
    }

    @Test
    void adds_amounts_in_the_same_currency() {
        assertThat(Money.of("1000.50", "GBP").plus(Money.of("99.50", "GBP")))
                .isEqualTo(Money.of("1100.00", "GBP"));
    }

    @Test
    void refuses_to_add_different_currencies() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of("1000", "INR").plus(Money.of("1000", "USD")))
                .withMessageContaining("different currencies");
    }

    @Test
    void multiplies_for_a_raise() {
        Money raised = Money.of("50000", "EUR").multipliedBy(new BigDecimal("1.08"));

        assertThat(raised).isEqualTo(Money.of("54000.00", "EUR"));
    }

    @Test
    void converts_using_a_supplied_rate() {
        Money inUsd = Money.of("830000", "INR").convertedTo("USD", new BigDecimal("0.012"));

        assertThat(inUsd).isEqualTo(Money.of("9960.00", "USD"));
    }

    @Test
    void rejects_a_non_positive_conversion_rate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of("100", "INR").convertedTo("USD", BigDecimal.ZERO))
                .withMessageContaining("rate must be positive");
    }

    @Test
    void reports_the_percentage_increase_over_a_previous_amount() {
        BigDecimal increase = Money.of("55000", "USD").percentageIncreaseOver(Money.of("50000", "USD"));

        assertThat(increase).isEqualByComparingTo("10.00");
    }

    @Test
    void reports_a_negative_percentage_for_a_pay_cut() {
        BigDecimal increase = Money.of("45000", "USD").percentageIncreaseOver(Money.of("50000", "USD"));

        assertThat(increase).isEqualByComparingTo("-10.00");
    }

    @Test
    void compares_amounts_in_the_same_currency() {
        assertThat(Money.of("60000", "SGD").isGreaterThan(Money.of("59999.99", "SGD"))).isTrue();
    }

    @Test
    void is_equal_regardless_of_how_the_amount_was_written() {
        assertThat(Money.of("100", "USD")).isEqualTo(Money.of("100.00", "USD"));
        assertThat(Money.of("100", "USD")).hasSameHashCodeAs(Money.of("100.00", "USD"));
    }

    @Test
    void is_not_equal_across_currencies() {
        assertThat(Money.of("100", "USD")).isNotEqualTo(Money.of("100", "CAD"));
    }
}
