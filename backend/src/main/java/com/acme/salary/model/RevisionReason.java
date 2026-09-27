package com.acme.salary.model;

/**
 * Why a salary changed. Recorded because "what does this person earn" is a much less useful
 * question than "why does this person earn it".
 */
public enum RevisionReason {
    HIRE,
    ANNUAL_RAISE,
    PROMOTION,
    MARKET_CORRECTION
}
