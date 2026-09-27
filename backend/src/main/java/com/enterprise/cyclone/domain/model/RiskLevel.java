package com.enterprise.cyclone.domain.model;

/**
 * Severity of predicted impact on a single asset.
 *
 * <p>Declaration order is ascending severity and is load-bearing: comparisons throughout the domain
 * rely on {@link Enum#compareTo(Object)} and {@link #atLeast(RiskLevel)}. New constants belong
 * between the existing ones, never at the end.
 */
public enum RiskLevel {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Whether this level is at least as severe as {@code other}.
     */
    public boolean atLeast(RiskLevel other) {
        return compareTo(other) >= 0;
    }
}
