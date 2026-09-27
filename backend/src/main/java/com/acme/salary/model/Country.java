package com.acme.salary.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The countries ACME employs people in, each with the currency it pays in.
 *
 * <p>An enum rather than a table: the list changes when the company opens an office, which is rare
 * and involves a code change anyway (seed data, exchange rates). If countries ever became
 * HR-editable this would have to become a table.
 */
@Getter
@RequiredArgsConstructor
public enum Country {

    INDIA("IN", "INR"),
    UNITED_STATES("US", "USD"),
    UNITED_KINGDOM("GB", "GBP"),
    GERMANY("DE", "EUR"),
    POLAND("PL", "PLN"),
    SINGAPORE("SG", "SGD"),
    AUSTRALIA("AU", "AUD"),
    CANADA("CA", "CAD"),
    BRAZIL("BR", "BRL"),
    JAPAN("JP", "JPY");

    private final String isoCode;

    /** The currency employees in this country are normally paid in. */
    private final String payCurrency;
}
