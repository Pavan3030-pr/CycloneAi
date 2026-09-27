package com.enterprise.cyclone.domain.model;

/**
 * Classification of a tracked critical infrastructure asset, used to weight impact and to drive
 * asset-specific resilience and evacuation rules.
 */
public enum AssetType {

    /** Generation, transmission or distribution node whose loss cascades beyond its footprint. */
    POWER_GRID,

    /** Primary arterial road segment, the backbone of evacuation and relief routing. */
    ARTERIAL_ROAD,

    /** Shelter of last resort with medical capability; highest protection priority. */
    MEDICAL_SHELTER
}
