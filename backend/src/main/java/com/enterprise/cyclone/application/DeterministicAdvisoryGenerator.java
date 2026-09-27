package com.enterprise.cyclone.application;

import com.enterprise.cyclone.application.port.AdvisoryGenerator;
import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.RiskLevel;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Template-based {@link AdvisoryGenerator} that needs no external service.
 *
 * <p>It is the default wiring and the fallback for any future model-backed generator: it is pure,
 * deterministic and fast enough to run inside a request, and it states the model's own limits in
 * the text it emits so that an operator never mistakes a screening score for a forecast.
 */
public final class DeterministicAdvisoryGenerator implements AdvisoryGenerator {

    private static final int MAX_LISTED_ASSETS = 5;
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    @Override
    public String generate(AdvisoryContext context) {
        Objects.requireNonNull(context, "context must not be null");

        StringBuilder advisory = new StringBuilder(1024);
        appendHeader(advisory, context);
        appendSummary(advisory, context.summary());
        appendPriorityAssets(advisory, context.exposures());
        appendBasis(advisory);
        return advisory.toString();
    }

    private static void appendHeader(StringBuilder advisory, AdvisoryContext context) {
        CycloneTrackPoint storm = context.stormPosition();
        SaffirSimpsonCategory category = context.category();
        advisory.append("CYCLONE IMPACT ADVISORY - ").append(context.stormId()).append('\n')
                .append(format("Valid at %s UTC\n", TIMESTAMP.format(context.evaluatedAt())))
                .append(format("Storm centre: latitude %.4f, longitude %.4f (GeoJSON position %s)\n",
                        storm.latitude(), storm.longitude(), storm.coordinateString()))
                .append(format("Intensity: %d kt (%s), central pressure %d mb\n",
                        storm.windSpeedKnots(), category.displayName(), storm.centralPressureMb()));
    }

    private static void appendSummary(StringBuilder advisory, ImpactSummary summary) {
        advisory.append(format("Assets assessed: %d\n", summary.totalAssets()))
                .append(format("Pre-landfall action required for: %d asset(s)\n", summary.actionableAssets()));

        RiskLevel[] levels = RiskLevel.values();
        for (int index = levels.length - 1; index >= 0; index--) {
            RiskLevel level = levels[index];
            advisory.append(format("  %-8s %d asset(s)\n", level, summary.countOf(level)));
        }
        advisory.append(format("Nearest asset: %.0f nm from the centre; peak modelled wind at an asset: %d kt\n",
                summary.nearestDistanceNauticalMiles(), summary.peakEstimatedWindKnots()));
    }

    private static void appendPriorityAssets(StringBuilder advisory, List<AssetExposure> exposures) {
        advisory.append("Priority assets:\n");

        int listed = Math.min(exposures.size(), MAX_LISTED_ASSETS);
        for (int index = 0; index < listed; index++) {
            AssetExposure exposure = exposures.get(index);
            advisory.append(format("  %d. [%s] %s (%s) - %.0f nm, %d kt: %s\n",
                    index + 1,
                    exposure.riskLevel(),
                    exposure.asset().name(),
                    exposure.asset().assetType(),
                    exposure.distanceNauticalMiles(),
                    exposure.estimatedWindAtAsset(),
                    exposure.rationale()));
        }
        if (exposures.size() > listed) {
            advisory.append(format("  ... and %d further asset(s) at lower exposure\n",
                    exposures.size() - listed));
        }
    }

    private static void appendBasis(StringBuilder advisory) {
        advisory.append("Basis: linear interpolation between published fixes with an exponential wind-field decay "
                + "(75 nm e-folding). Terrain, gust factor, quadrant asymmetry and forecast positional "
                + "uncertainty are not modelled. Cross-check with official NHC/JTWC products before "
                + "operational use.");
    }

    private static String format(String template, Object... arguments) {
        return String.format(Locale.ROOT, template, arguments);
    }
}
