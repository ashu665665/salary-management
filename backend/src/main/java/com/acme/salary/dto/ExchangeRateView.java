package com.acme.salary.dto;

import java.math.BigDecimal;

/** A rate the reported figures were converted at, so a total can be traced back to it. */
public record ExchangeRateView(String currency, BigDecimal unitsPerUsd) {
}
