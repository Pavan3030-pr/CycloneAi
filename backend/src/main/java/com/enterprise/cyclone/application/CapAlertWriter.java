package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.RiskLevel;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Renders an assessment as a Common Alerting Protocol 1.2 alert.
 *
 * <p>This is the interoperability payoff: CAP is the format India's NDMA/SACHET pipelines and most
 * national warning systems already exchange, so an alert produced here can be consumed by a state
 * emergency operations centre, a cell-broadcast gateway or an aggregator without anyone writing a
 * bespoke integration. A PDF or a JSON blob would have been easier and worth far less.
 *
 * <p>The document is built to the CAP 1.2 element sequence, which is ordered rather than free-form:
 * a schema-aware consumer rejects a document whose elements are out of order even when every field is
 * present. Element order here therefore mirrors the specification's {@code alert} and {@code info}
 * sequences exactly, and the accompanying test asserts the structure parses.
 *
 * <p>Identifiers are deterministic — storm, evaluation instant and language — so re-issuing the same
 * assessment produces the same alert id, which lets a downstream system de-duplicate a warning that
 * has been re-published rather than treating it as a second event.
 */
public final class CapAlertWriter {

    private static final String NAMESPACE = "urn:oasis:names:tc:emergency:cap:1.2";

    /** Distance around the storm centre published as the alert area: the outermost screening band. */
    private static final double SCREENING_BAND_NM = 120.0;

    private static final double NAUTICAL_MILES_TO_KILOMETRES = 1.852;

    private static final DateTimeFormatter CAP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter IDENTIFIER_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);

    /**
     * Publication details that are deployment configuration rather than domain facts.
     *
     * @param sender the originating system's address, non-blank
     * @param senderName the human-readable name of the issuing organisation, non-blank
     * @param validity how long the alert remains valid from the evaluation instant
     * @param sentAt when the alert is issued, from the injected clock
     */
    public record Options(String sender, String senderName, Duration validity, Instant sentAt) {

        public Options {
            if (sender == null || sender.isBlank()) {
                throw new IllegalArgumentException("sender must not be blank");
            }
            if (senderName == null || senderName.isBlank()) {
                throw new IllegalArgumentException("senderName must not be blank");
            }
            Objects.requireNonNull(validity, "validity must not be null");
            if (validity.isNegative() || validity.isZero()) {
                throw new IllegalArgumentException("validity must be positive");
            }
            Objects.requireNonNull(sentAt, "sentAt must not be null");
        }
    }

    /**
     * Renders one alert.
     *
     * @param report the assessment to publish, non-null
     * @param language the language of the text elements, non-null
     * @param options publication details, non-null
     * @return a complete, well-formed CAP 1.2 document
     */
    public String write(ImpactReport report, AdvisoryLanguage language, Options options) {
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(options, "options must not be null");

        int actionable = report.actionableExposures().size();
        RiskLevel highest = report.summary().highestRisk();
        Instant effective = report.evaluatedAt();
        Instant expires = effective.plus(options.validity());
        double radiusKm = SCREENING_BAND_NM * NAUTICAL_MILES_TO_KILOMETRES;

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        xml.append("<alert xmlns=\"").append(NAMESPACE).append("\">\n");

        // alert sequence: identifier, sender, sent, status, msgType, source, scope, info+
        element(xml, 1, "identifier", identifier(report, language));
        element(xml, 1, "sender", options.sender());
        element(xml, 1, "sent", CAP_TIMESTAMP.format(options.sentAt()));
        element(xml, 1, "status", "Actual");
        element(xml, 1, "msgType", "Alert");
        element(xml, 1, "source", options.senderName());
        element(xml, 1, "scope", "Public");

        xml.append("  <info>\n");
        // info sequence: language, category+, event, responseType*, urgency, severity, certainty,
        // effective, onset, expires, senderName, headline, description, instruction, contact,
        // parameter*, area*
        element(xml, 2, "language", language.code());
        element(xml, 2, "category", "Met");
        element(xml, 2, "event", AdvisoryVocabulary.event(language));
        element(xml, 2, "responseType", "Shelter");
        element(xml, 2, "urgency", urgencyFor(highest));
        element(xml, 2, "severity", severityFor(highest));
        element(xml, 2, "certainty", "Likely");
        element(xml, 2, "effective", CAP_TIMESTAMP.format(effective));
        element(xml, 2, "onset", CAP_TIMESTAMP.format(effective));
        element(xml, 2, "expires", CAP_TIMESTAMP.format(expires));
        element(xml, 2, "senderName", options.senderName());
        element(xml, 2, "headline", AdvisoryVocabulary.headline(report.stormId(), actionable, highest, language));
        element(xml, 2, "description", report.advisory());
        element(xml, 2, "instruction", AdvisoryVocabulary.instruction(language));
        element(xml, 2, "contact", AdvisoryVocabulary.contact(language));

        parameter(xml, "stormId", report.stormId());
        parameter(xml, "evaluatedAt", CAP_TIMESTAMP.format(effective));
        parameter(xml, "assetsAssessed", Integer.toString(report.summary().totalAssets()));
        parameter(xml, "assetsRequiringAction", Integer.toString(actionable));
        parameter(xml, "intensityKt", Integer.toString(report.stormPosition().windSpeedKnots()));
        parameter(xml, "centralPressureMb", Integer.toString(report.stormPosition().centralPressureMb()));
        parameter(xml, "nearestAssetNm", String.format(Locale.ROOT, "%.2f", report.summary().nearestDistanceNauticalMiles()));
        for (RiskLevel level : RiskLevel.values()) {
            parameter(xml, "count." + level.name(), Integer.toString(report.summary().countOf(level)));
        }
        parameter(xml, "modelBasis", "75 nm exponential wind decay; no terrain, gust or quadrant modelling");

        xml.append("    <area>\n");
        element(xml, 3, "areaDesc", AdvisoryVocabulary.areaDescription(
                radiusKm, report.stormPosition().latitude(), report.stormPosition().longitude(), language));
        element(xml, 3, "circle", String.format(
                Locale.ROOT,
                "%.4f,%.4f %.2f",
                report.stormPosition().latitude(),
                report.stormPosition().longitude(),
                radiusKm));
        xml.append("    </area>\n");
        xml.append("  </info>\n");
        xml.append("</alert>\n");

        return xml.toString();
    }

    /** The published identifier, stable for the same storm, evaluation instant and language. */
    public String identifier(ImpactReport report, AdvisoryLanguage language) {
        return "cycloneai-" + report.stormId() + "-" + language.code() + "-"
                + IDENTIFIER_TIMESTAMP.format(report.evaluatedAt());
    }

    private static String severityFor(RiskLevel highest) {
        return switch (highest) {
            case CRITICAL -> "Extreme";
            case HIGH -> "Severe";
            case MEDIUM -> "Moderate";
            case LOW -> "Minor";
        };
    }

    private static String urgencyFor(RiskLevel highest) {
        return switch (highest) {
            case CRITICAL, HIGH -> "Immediate";
            case MEDIUM -> "Expected";
            case LOW -> "Future";
        };
    }

    private static void element(StringBuilder xml, int depth, String name, String value) {
        xml.append("  ".repeat(depth))
                .append('<').append(name).append('>')
                .append(escape(value))
                .append("</").append(name).append(">\n");
    }

    private static void parameter(StringBuilder xml, String name, String value) {
        xml.append("    <parameter>\n");
        element(xml, 3, "valueName", name);
        element(xml, 3, "value", value);
        xml.append("    </parameter>\n");
    }

    /**
     * Escapes XML text.
     *
     * <p>The advisory contains {@code &} and comparison characters in its own wording, and the
     * rationale strings cite thresholds such as {@code <= 30 nm}; an unescaped one of those produces
     * a document that no parser will read, at the exact moment it matters.
     */
    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
