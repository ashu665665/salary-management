package com.acme.salary.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Seniority, used to group peers when comparing pay.
 *
 * <p>The rank is explicit rather than taken from the enum ordinal, so reordering or inserting a
 * level cannot silently change what counts as a promotion.
 */
@Getter
@RequiredArgsConstructor
public enum JobLevel {

    JUNIOR(1),
    MID(2),
    SENIOR(3),
    LEAD(4),
    MANAGER(5),
    DIRECTOR(6),
    EXECUTIVE(7);

    private final int rank;

    public boolean isAbove(JobLevel other) {
        return rank > other.rank;
    }
}
