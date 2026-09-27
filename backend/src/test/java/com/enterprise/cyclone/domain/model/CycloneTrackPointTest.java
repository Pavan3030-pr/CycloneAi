package com.enterprise.cyclone.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class CycloneTrackPointTest {

    private static final Instant VALID_TIME = Instant.parse("2026-09-27T06:00:00Z");

    @Test
    void exposesFeedAlignedValues() {
        CycloneTrackPoint point = new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME);

        assertThat(point.latitude()).isEqualTo(18.4);
        assertThat(point.longitude()).isEqualTo(-132.7);
        assertThat(point.windSpeedKnots()).isEqualTo(115);
        assertThat(point.centralPressureMb()).isEqualTo(940);
        assertThat(point.timestamp()).isEqualTo(VALID_TIME);
    }

    @Test
    void rendersCoordinateInGeoJsonLongitudeFirstOrder() {
        CycloneTrackPoint point = new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME);

        assertThat(point.coordinateString()).isEqualTo("-132.7000,18.4000");
    }

    @Test
    void coordinateStringIsIndependentOfDefaultLocale() {
        CycloneTrackPoint point = new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME);
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);

            assertThat(point.coordinateString()).isEqualTo("-132.7000,18.4000");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void convertsToGeoCoordinateValueObject() {
        CycloneTrackPoint point = new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME);

        assertThat(point.coordinate()).isEqualTo(new GeoCoordinate(18.4, -132.7));
    }

    @Test
    void acceptsEquatorAndAntiMeridianBoundaries() {
        assertThat(new CycloneTrackPoint(0.0, 180.0, 0, 1005, VALID_TIME).coordinateString())
                .isEqualTo("180.0000,0.0000");
        assertThat(new CycloneTrackPoint(-90.0, -180.0, 25, 1000, VALID_TIME).coordinateString())
                .isEqualTo("-180.0000,-90.0000");
    }

    @Test
    void rejectsLatitudeOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(90.1, 0.0, 115, 940, VALID_TIME))
                .withMessageContaining("latitude");
    }

    @Test
    void rejectsNonFiniteLatitude() {
        // NaN slips past naive range comparisons, so it must be rejected explicitly.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(Double.NaN, 0.0, 115, 940, VALID_TIME))
                .withMessageContaining("latitude");
    }

    @Test
    void rejectsLongitudeOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -180.1, 115, 940, VALID_TIME))
                .withMessageContaining("longitude");
    }

    @Test
    void rejectsWindSpeedOutsideSanityBounds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -132.7, -1, 940, VALID_TIME))
                .withMessageContaining("windSpeedKnots");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -132.7, 251, 940, VALID_TIME))
                .withMessageContaining("windSpeedKnots");
    }

    @Test
    void rejectsCentralPressureOutsideSanityBounds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -132.7, 115, 849, VALID_TIME))
                .withMessageContaining("centralPressureMb");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -132.7, 115, 1026, VALID_TIME))
                .withMessageContaining("centralPressureMb");
    }

    @Test
    void rejectsNullTimestamp() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CycloneTrackPoint(18.4, -132.7, 115, 940, null))
                .withMessageContaining("timestamp");
    }

    @Test
    void hasValueSemantics() {
        CycloneTrackPoint point = new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME);

        assertThat(point).isEqualTo(new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME));
        assertThat(point).hasSameHashCodeAs(new CycloneTrackPoint(18.4, -132.7, 115, 940, VALID_TIME));
    }
}
