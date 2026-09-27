package com.enterprise.cyclone.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CycloneTrackTest {

    private static final Instant T00 = Instant.parse("2026-09-27T00:00:00Z");
    private static final Instant T06 = Instant.parse("2026-09-27T06:00:00Z");
    private static final Instant T12 = Instant.parse("2026-09-27T12:00:00Z");
    private static final Instant T18 = Instant.parse("2026-09-27T18:00:00Z");

    private static CycloneTrackPoint point(Instant timestamp, double latitude) {
        return new CycloneTrackPoint(latitude, 132.7, 100, 960, timestamp);
    }

    @Test
    void sortsPointsIntoStrictChronologicalOrder() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(
                point(T12, 15.0),
                point(T00, 13.0),
                point(T18, 16.0),
                point(T06, 14.0)));

        assertThat(track.points()).extracting(CycloneTrackPoint::timestamp)
                .containsExactly(T00, T06, T12, T18);
    }

    @Test
    void exposesTimelineBoundaries() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T12, 15.0), point(T00, 13.0)));

        assertThat(track.startTime()).isEqualTo(T00);
        assertThat(track.endTime()).isEqualTo(T12);
    }

    @Test
    void boundariesCollapseOnASinglePointTrack() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T06, 14.0)));

        assertThat(track.startTime()).isEqualTo(track.endTime()).isEqualTo(T06);
    }

    @Test
    void copiesTheIncomingListSoLaterMutationIsInvisible() {
        List<CycloneTrackPoint> source = new ArrayList<>(List.of(point(T06, 14.0)));
        CycloneTrack track = new CycloneTrack("WP0724", source);

        source.add(point(T12, 15.0));
        source.clear();

        assertThat(track.points()).hasSize(1);
        assertThat(track.endTime()).isEqualTo(T06);
    }

    @Test
    void exposesAnUnmodifiableTimeline() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T00, 13.0)));

        assertThatThrownBy(() -> track.points().add(point(T06, 14.0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findsTheFixAtAnExactTimestamp() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(
                point(T06, 14.0), point(T00, 13.0), point(T18, 16.0), point(T12, 15.0)));

        assertThat(track.pointAt(T12)).contains(point(T12, 15.0));
    }

    @Test
    void findsBoundaryFixesRegardlessOfRank() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T00, 13.0), point(T06, 14.0), point(T12, 15.0)));

        assertThat(track.pointAt(T00)).isPresent();
        assertThat(track.pointAt(T12)).isPresent();
    }

    @Test
    void returnsEmptyForInstantsWithoutAFix() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T00, 13.0), point(T12, 15.0)));

        assertThat(track.pointAt(T06)).isEmpty();
        assertThat(track.pointAt(T00.minusSeconds(1))).isEmpty();
        assertThat(track.pointAt(T12.plusSeconds(1))).isEmpty();
    }

    @Test
    void rejectsNullLookupInstant() {
        CycloneTrack track = new CycloneTrack("WP0724", List.of(point(T00, 13.0)));

        assertThatNullPointerException()
                .isThrownBy(() -> track.pointAt(null))
                .withMessageContaining("time");
    }

    @Test
    void rejectsDuplicateTimestampsWithAnInformativeMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrack("WP0724", List.of(point(T06, 14.0), point(T06, 15.5))))
                .withMessageContaining("WP0724")
                .withMessageContaining(T06.toString())
                .withMessageContaining("multiple track points");
    }

    @Test
    void rejectsNullPointsList() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CycloneTrack("WP0724", null))
                .withMessageContaining("points");
    }

    @Test
    void rejectsMissingOrBlankStormId() {
        // Consistent with InfrastructureAsset: a missing identity is a contract violation (IAE),
        // while a missing collection is a programming error (NPE).
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrack(null, List.of(point(T00, 13.0))))
                .withMessageContaining("stormId");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrack("  ", List.of(point(T00, 13.0))))
                .withMessageContaining("stormId");
    }

    @Test
    void stripsSurroundingWhitespaceFromStormId() {
        CycloneTrack track = new CycloneTrack("  WP0724  ", List.of(point(T00, 13.0)));

        assertThat(track.stormId()).isEqualTo("WP0724");
    }

    @Test
    void rejectsAnEmptyTimeline() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CycloneTrack("WP0724", List.of()))
                .withMessageContaining("at least one track point");
    }

    @Test
    void rejectsNullElementsInTheTimeline() {
        List<CycloneTrackPoint> points = new ArrayList<>();
        points.add(point(T00, 13.0));
        points.add(null);

        assertThatNullPointerException()
                .isThrownBy(() -> new CycloneTrack("WP0724", points))
                .withMessageContaining("index 1");
    }

    @Test
    void hasValueSemanticsIndependentOfInputOrder() {
        CycloneTrack ascending = new CycloneTrack("WP0724", List.of(point(T00, 13.0), point(T06, 14.0)));
        CycloneTrack descending = new CycloneTrack("WP0724", List.of(point(T06, 14.0), point(T00, 13.0)));

        assertThat(ascending).isEqualTo(descending);
        assertThat(ascending).hasSameHashCodeAs(descending);
    }

    @Test
    void acceptsARealisticMultiPointBestTrack() {
        List<CycloneTrackPoint> fixes = List.of(
                new CycloneTrackPoint(11.2, 141.8, 35, 1002, Instant.parse("2026-09-25T00:00:00Z")),
                new CycloneTrackPoint(12.6, 139.4, 55, 990, Instant.parse("2026-09-25T12:00:00Z")),
                new CycloneTrackPoint(14.1, 136.2, 85, 970, Instant.parse("2026-09-26T00:00:00Z")),
                new CycloneTrackPoint(16.0, 133.5, 115, 940, Instant.parse("2026-09-26T12:00:00Z")),
                new CycloneTrackPoint(18.4, 132.7, 125, 925, Instant.parse("2026-09-27T00:00:00Z")));

        CycloneTrack track = new CycloneTrack("WP0726", fixes);

        assertThat(track.points()).hasSize(5);
        assertThat(track.startTime()).isEqualTo(Instant.parse("2026-09-25T00:00:00Z"));
        assertThat(track.endTime()).isEqualTo(Instant.parse("2026-09-27T00:00:00Z"));
        assertThat(track.pointAt(Instant.parse("2026-09-26T12:00:00Z")))
                .get()
                .extracting(CycloneTrackPoint::windSpeedKnots)
                .isEqualTo(115);
    }
}
