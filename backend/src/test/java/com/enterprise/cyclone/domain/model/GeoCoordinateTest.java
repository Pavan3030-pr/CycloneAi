package com.enterprise.cyclone.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeoCoordinateTest {

    @Test
    void rendersPositionArrayInLongitudeFirstOrder() {
        GeoCoordinate coordinate = new GeoCoordinate(14.5995, 120.9842);

        assertThat(coordinate.asPositionArray()).containsExactly(120.9842, 14.5995);
        assertThat(coordinate.asCoordinateString()).isEqualTo("120.9842,14.5995");
    }

    @Test
    void formatsNegativeCoordinatesWithRootLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ITALY);

            assertThat(GeoCoordinate.coordinateString(-132.7, -18.4)).isEqualTo("-132.7000,-18.4000");
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.1, 90.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidLatitude(double latitude) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GeoCoordinate(latitude, 0.0))
                .withMessageContaining("latitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.1, 180.1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidLongitude(double longitude) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GeoCoordinate(0.0, longitude))
                .withMessageContaining("longitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.0, 0.0, 90.0})
    void acceptsLatitudeBoundaries(double latitude) {
        assertThat(new GeoCoordinate(latitude, 0.0).latitude()).isEqualTo(latitude);
    }

    @Test
    void computesGreatCircleDistanceInNauticalMiles() {
        // One degree of latitude along a meridian is ~60 nm by definition.
        GeoCoordinate origin = new GeoCoordinate(0.0, 0.0);

        assertThat(origin.distanceNauticalMilesTo(new GeoCoordinate(1.0, 0.0))).isCloseTo(60.0, within(0.1));
    }

    @Test
    void rejectsNullDistanceTarget() {
        assertThatNullPointerException()
                .isThrownBy(() -> new GeoCoordinate(0.0, 0.0).distanceNauticalMilesTo(null))
                .withMessageContaining("other");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
