package com.enterprise.cyclone.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class InfrastructureAssetTest {

    private static final GeoCoordinate MANILA_BAY = new GeoCoordinate(14.5995, 120.9842);

    @Test
    void exposesAssetAttributes() {
        InfrastructureAsset asset =
                new InfrastructureAsset("US-NERC-1042", "Bay Substation 7", AssetType.POWER_GRID, MANILA_BAY);

        assertThat(asset.id()).isEqualTo("US-NERC-1042");
        assertThat(asset.name()).isEqualTo("Bay Substation 7");
        assertThat(asset.assetType()).isEqualTo(AssetType.POWER_GRID);
        assertThat(asset.coordinate()).isEqualTo(MANILA_BAY);
    }

    @Test
    void stripsSurroundingWhitespaceFromIdentity() {
        InfrastructureAsset asset =
                new InfrastructureAsset("  ID-7  ", "  Coastal Clinic  ", AssetType.MEDICAL_SHELTER, MANILA_BAY);

        assertThat(asset.id()).isEqualTo("ID-7");
        assertThat(asset.name()).isEqualTo("Coastal Clinic");
    }

    @Test
    void rendersCoordinateInGeoJsonLongitudeFirstOrder() {
        InfrastructureAsset asset =
                new InfrastructureAsset("ID-7", "Coastal Clinic", AssetType.MEDICAL_SHELTER, MANILA_BAY);

        assertThat(asset.coordinateString()).isEqualTo("120.9842,14.5995");
    }

    @Test
    void definesTheThreeTrackedAssetTypes() {
        assertThat(AssetType.values())
                .containsExactly(AssetType.POWER_GRID, AssetType.ARTERIAL_ROAD, AssetType.MEDICAL_SHELTER);
    }

    @Test
    void rejectsBlankIdentity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InfrastructureAsset("  ", "Bay Substation 7", AssetType.POWER_GRID, MANILA_BAY))
                .withMessageContaining("id");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InfrastructureAsset("ID-7", null, AssetType.POWER_GRID, MANILA_BAY))
                .withMessageContaining("name");
    }

    @Test
    void rejectsMissingTypeOrCoordinate() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InfrastructureAsset("ID-7", "Bay Substation 7", null, MANILA_BAY))
                .withMessageContaining("assetType");
        assertThatNullPointerException()
                .isThrownBy(() -> new InfrastructureAsset("ID-7", "Bay Substation 7", AssetType.POWER_GRID, null))
                .withMessageContaining("coordinate");
    }

    @Test
    void hasValueSemantics() {
        InfrastructureAsset asset =
                new InfrastructureAsset("ID-7", "Coastal Clinic", AssetType.MEDICAL_SHELTER, MANILA_BAY);

        assertThat(asset).isEqualTo(
                new InfrastructureAsset("ID-7", "Coastal Clinic", AssetType.MEDICAL_SHELTER, MANILA_BAY));
        assertThat(asset).hasSameHashCodeAs(
                new InfrastructureAsset("ID-7", "Coastal Clinic", AssetType.MEDICAL_SHELTER, MANILA_BAY));
    }
}
