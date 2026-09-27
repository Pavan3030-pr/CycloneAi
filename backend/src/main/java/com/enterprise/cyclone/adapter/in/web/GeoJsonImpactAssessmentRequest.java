package com.enterprise.cyclone.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Request to assess a published GeoJSON track product directly, as NOAA/NHC and JTWC serve it.
 *
 * @param featureCollection the upstream GeoJSON feature collection, non-null and unmodified; its
 *        total size is bounded by the payload limit and its shape by the JSON parser constraints
 * @param assets the assets to assess, non-null, non-empty and at most 500 entries
 * @param evaluationTime the instant to evaluate, or null to evaluate at the track's latest fix
 * @param language the language to publish the advisory in, or null for English
 */
public record GeoJsonImpactAssessmentRequest(
        @NotNull JsonNode featureCollection,
        @NotNull @NotEmpty @Size(max = 500) @Valid List<AssetRequest> assets,
        Instant evaluationTime,
        @Pattern(regexp = "(?i)en|hi|te", message = "must be one of en, hi or te")
        @Size(max = 8) String language) {
}
