package com.acme.salary.dto;

import com.acme.salary.model.Money;
import java.math.BigDecimal;

/** An amount and its currency, as the API renders them. */
public record MoneyView(BigDecimal amount, String currency) {

    public static MoneyView of(Money money) {
        return money == null ? null : new MoneyView(money.getAmount(), money.getCurrency());
    }
}
