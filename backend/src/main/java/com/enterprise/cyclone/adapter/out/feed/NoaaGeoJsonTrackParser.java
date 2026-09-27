package com.enterprise.cyclone.adapter.out.feed;

import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Maps the GeoJSON track products published by NOAA/NHC and JTWC onto the domain model.
 *
 * <p>The upstream feature collections are heterogeneous: forecast and best-track positions arrive as
 * {@code Point} features, while the drawn track and the wind-cone surfaces arrive as {@code
 * LineString} and {@code Polygon} features in the same collection. Only point fixes can enter a
 * {@link CycloneTrack}, so the geometry is classified explicitly and everything else is skipped and
 * recorded for diagnostics.
 *
 * <p>Property names differ between products and between agency revisions, so each required value is
 * read from an ordered list of accepted names &mdash; for example the maximum sustained wind is
 * taken from NHC's {@code maxwind} or the equivalent {@code intensity}, {@code vmax} or
 * {@code windSpeedKnots}. Timestamps may be RFC 3339 ({@code 2026-09-27T06:00:00Z}) or the ATCF
 * compact forms {@code yyyyMMddHH} and {@code yyyyMMddHHmm}, which are always UTC.
 *
 * <p>Every failure is reported as an {@link IllegalArgumentException} naming the offending feature
 * index and the accepted property names. Values are never defaulted, because a silently zeroed
 * longitude places a storm in the Gulf of Guinea.
 */
public final class NoaaGeoJsonTrackParser {

    private static final String FEATURE_COLLECTION = "FeatureCollection";
    private static final String POINT_GEOMETRY = "Point";

    private static final int ATCF_HOUR_LENGTH = 10;
    private static final int ATCF_MINUTE_LENGTH = 12;
    private static final DateTimeFormatter ATCF_HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH", Locale.ROOT);
    private static final DateTimeFormatter ATCF_MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.ROOT);

    private static final List<String> STORM_ID_FIELDS = List.of("stormid", "stormId", "id");
    private static final List<String> WIND_FIELDS = List.of("maxwind", "intensity", "vmax", "windSpeedKnots");
    private static final List<String> PRESSURE_FIELDS = List.of("mslp", "pressure", "minpressure", "centralPressureMb");
    private static final List<String> TIME_FIELDS = List.of("validtime", "valid", "timestamp", "time");

    private final ObjectMapper objectMapper;

    public NoaaGeoJsonTrackParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Parses a GeoJSON feature collection document.
     *
     * @param geoJsonDocument the raw upstream document, non-null
     * @return the storm timeline, canonically ordered by the {@link CycloneTrack} constructor
     * @throws NullPointerException if {@code geoJsonDocument} is null
     * @throws IllegalArgumentException if the document is empty, not valid JSON, not a feature
     *         collection, contains no point feature, or holds a feature with a missing or malformed
     *         position, wind, pressure or time
     */
    public CycloneTrack parse(String geoJsonDocument) {
        Objects.requireNonNull(geoJsonDocument, "geoJsonDocument must not be null");

        JsonNode document;
        try {
            document = objectMapper.readTree(geoJsonDocument);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("document is not valid JSON: " + ex.getOriginalMessage(), ex);
        }
        if (document == null) {
            throw new IllegalArgumentException("document is empty and cannot form a track");
        }
        return parse(document);
    }

    /**
     * Parses an already-deserialised GeoJSON feature collection.
     *
     * @param featureCollection the parsed document, non-null
     * @return the storm timeline
     * @throws NullPointerException if {@code featureCollection} is null
     * @throws IllegalArgumentException as described for {@link #parse(String)}
     */
    public CycloneTrack parse(JsonNode featureCollection) {
        Objects.requireNonNull(featureCollection, "featureCollection must not be null");
        requireType(featureCollection, "document");

        JsonNode features = featureCollection.path("features");
        if (!features.isArray() || features.isEmpty()) {
            throw new IllegalArgumentException("feature collection must carry a non-empty 'features' array");
        }

        String stormId = null;
        List<CycloneTrackPoint> points = new ArrayList<>(features.size());
        Set<String> skippedGeometryTypes = new LinkedHashSet<>();

        for (int index = 0; index < features.size(); index++) {
            JsonNode feature = features.get(index);
            switch (classify(feature.path("geometry"))) {
                case TrackFixPosition fix -> {
                    JsonNode properties = feature.path("properties");
                    if (stormId == null) {
                        stormId = requiredText(properties, STORM_ID_FIELDS, index, "storm id");
                    }
                    points.add(toTrackPoint(properties, fix.coordinate(), index));
                }
                case IgnoredGeometry ignored -> skippedGeometryTypes.add(ignored.geoJsonType());
            }
        }

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                    "feature collection holds no '" + POINT_GEOMETRY + "' geometry and cannot form a track; "
                            + "observed geometry types: " + skippedGeometryTypes);
        }
        return new CycloneTrack(stormId, points);
    }

    private static void requireType(JsonNode document, String description) {
        String type = document.path("type").asText("");
        if (!FEATURE_COLLECTION.equals(type)) {
            throw new IllegalArgumentException(
                    description + " must be a GeoJSON '" + FEATURE_COLLECTION + "' but was '" + type + "'");
        }
    }

    /**
     * Classifies a GeoJSON geometry node. Only point fixes can be expressed as domain positions.
     */
    private static FeatureGeometry classify(JsonNode geometry) {
        if (!POINT_GEOMETRY.equals(geometry.path("type").asText(""))) {
            return new IgnoredGeometry(geometry.path("type").asText(""));
        }
        return new TrackFixPosition(position(geometry));
    }

    /**
     * Reads a GeoJSON position, which RFC 7946 orders as {@code [longitude, latitude]}.
     */
    private static GeoCoordinate position(JsonNode geometry) {
        JsonNode coordinates = geometry.path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2) {
            throw new IllegalArgumentException(
                    "a '" + POINT_GEOMETRY + "' geometry must carry a 'coordinates' array of [longitude, latitude]");
        }

        JsonNode longitudeNode = coordinates.get(0);
        JsonNode latitudeNode = coordinates.get(1);
        if (!longitudeNode.isNumber() || !latitudeNode.isNumber()) {
            throw new IllegalArgumentException("a '" + POINT_GEOMETRY + "' geometry must carry numeric coordinates but was "
                    + coordinates);
        }
        return new GeoCoordinate(latitudeNode.asDouble(), longitudeNode.asDouble());
    }

    private static CycloneTrackPoint toTrackPoint(JsonNode properties, GeoCoordinate position, int index) {
        return new CycloneTrackPoint(
                position.latitude(),
                position.longitude(),
                requiredInteger(properties, WIND_FIELDS, index, "a maximum sustained wind in knots"),
                requiredInteger(properties, PRESSURE_FIELDS, index, "a central pressure in mb"),
                timestamp(requiredText(properties, TIME_FIELDS, index, "a valid time"), index));
    }

    private static String requiredText(JsonNode properties, List<String> fieldNames, int index, String description) {
        for (String fieldName : fieldNames) {
            JsonNode value = properties.get(fieldName);
            if (value != null && value.isValueNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText().strip();
            }
        }
        throw new IllegalArgumentException(
                "feature " + index + " is missing " + description + "; accepted properties: " + fieldNames);
    }

    private static int requiredInteger(JsonNode properties, List<String> fieldNames, int index, String description) {
        for (String fieldName : fieldNames) {
            JsonNode value = properties.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (!value.isNumber()) {
                throw new IllegalArgumentException("feature " + index + " property '" + fieldName
                        + "' must be numeric but was '" + value.asText() + "'");
            }
            return value.intValue();
        }
        throw new IllegalArgumentException(
                "feature " + index + " is missing " + description + "; accepted properties: " + fieldNames);
    }

    /**
     * Parses an RFC 3339 instant or an ATCF compact UTC timestamp.
     */
    private static Instant timestamp(String rawValue, int index) {
        String value = rawValue.strip();
        try {
            if (value.indexOf('T') > 0) {
                return Instant.parse(value);
            }
            if (value.length() == ATCF_HOUR_LENGTH) {
                return LocalDateTime.parse(value, ATCF_HOUR).toInstant(ZoneOffset.UTC);
            }
            if (value.length() == ATCF_MINUTE_LENGTH) {
                return LocalDateTime.parse(value, ATCF_MINUTE).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "feature " + index + " has an unparseable time value '" + rawValue + "'", ex);
        }
        throw new IllegalArgumentException("feature " + index + " has an unsupported time value '" + rawValue
                + "'; expected RFC 3339 or ATCF yyyyMMddHH[mm]");
    }

    /**
     * Geometry of one upstream feature: either a position usable as a fix, or a shape the timeline
     * cannot accept. Sealing the hierarchy makes the two cases exhaustive at compile time, so a new
     * geometry kind cannot be added without a decision about it here.
     */
    private sealed interface FeatureGeometry permits TrackFixPosition, IgnoredGeometry {
    }

    private record TrackFixPosition(GeoCoordinate coordinate) implements FeatureGeometry {
    }

    private record IgnoredGeometry(String geoJsonType) implements FeatureGeometry {
    }
}
