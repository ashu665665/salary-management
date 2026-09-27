package com.acme.salary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An amount of money in a specific currency.
 *
 * <p>Salaries are held in the employee's local currency, so every amount has to carry its currency
 * with it. Keeping the two together in one type means we cannot accidentally add a rupee figure to a
 * euro figure, or report a total that silently mixes both.
 *
 * <p>The scale follows the currency: two decimal places for USD, none for JPY.
 *
 * <p>Equality is written out by hand rather than generated, because {@code BigDecimal.equals}
 * compares scale as well as value, which would make 100 and 100.00 different amounts.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class Money implements Comparable<Money> {

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount is required");
        Currency resolved = resolveCurrency(currencyCode);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative: " + amount);
        }
        return new Money(applyCurrencyScale(amount, resolved), resolved.getCurrencyCode());
    }

    public static Money of(String amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount is required");
        return of(new BigDecimal(amount), currencyCode);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return Money.of(amount.add(other.amount), currency);
    }

    public Money multipliedBy(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor is required");
        return Money.of(amount.multiply(factor), currency);
    }

    /**
     * Converts to another currency using a supplied rate. The rate is passed in rather than looked
     * up so that conversion stays a pure calculation: the caller decides which rate set applies.
     */
    public Money convertedTo(String targetCurrency, BigDecimal rate) {
        Objects.requireNonNull(rate, "rate is required");
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive: " + rate);
        }
        return Money.of(amount.multiply(rate), targetCurrency);
    }

    /**
     * The percentage increase from an earlier amount, e.g. 50,000 to 55,000 is 10.00.
     */
    public BigDecimal percentageIncreaseOver(Money previous) {
        requireSameCurrency(previous);
        if (previous.amount.signum() == 0) {
            throw new IllegalArgumentException("cannot express an increase over zero");
        }
        return amount.subtract(previous.amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.amount, 2, RoundingMode.HALF_UP);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other amount is required");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "cannot combine amounts in different currencies: " + currency + " and " + other.currency);
        }
    }

    private static Currency resolveCurrency(String code) {
        Objects.requireNonNull(code, "currency is required");
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown currency code: " + code, e);
        }
    }

    private static BigDecimal applyCurrencyScale(BigDecimal amount, Currency currency) {
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        return amount.setScale(fractionDigits, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return amount.compareTo(other.amount) == 0 && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
