package com.enterprise.cyclone.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The CAP 1.2 document, tested as a document rather than as a string.
 *
 * <p>CAP is a standards format with an ordered element sequence, and a consumer that validates
 * against the schema rejects a document whose elements are merely in the wrong order even when every
 * value is right. These tests parse the output with a real parser, assert the sequence the
 * specification requires, and check that an advisory containing {@code &} or {@code <} — which the
 * model's basis sentence and the rule citations both do — still produces a document that parses.
 */
class CapAlertWriterTest {

    private static final CapAlertWriter WRITER = new CapAlertWriter();
    private static final Instant SENT_AT = Instant.parse("2023-12-05T07:30:00Z");

    @Test
    void producesAWellFormedCapAlertInTheCapNamespace() throws Exception {
        Document document = parse(WRITER.write(report(RiskLevel.CRITICAL), AdvisoryLanguage.EN, options()));

        Element alert = document.getDocumentElement();
        assertThat(alert.getTagName()).isEqualTo("alert");
        assertThat(alert.getNamespaceURI()).isEqualTo("urn:oasis:names:tc:emergency:cap:1.2");

        // The alert sequence, in the order CAP 1.2 defines it.
        assertThat(childNames(alert))
                .containsExactly("identifier", "sender", "sent", "status", "msgType", "source", "scope", "info");
        assertThat(text(alert, "status")).isEqualTo("Actual");
        assertThat(text(alert, "msgType")).isEqualTo("Alert");
        assertThat(text(alert, "scope")).isEqualTo("Public");
        assertThat(text(alert, "sent")).isEqualTo("2023-12-05T07:30:00Z");
    }

    @Test
    void infoBlockFollowsTheCapSequence() throws Exception {
        Document document = parse(WRITER.write(report(RiskLevel.HIGH), AdvisoryLanguage.EN, options()));
        Element info = (Element) document.getElementsByTagName("info").item(0);

        assertThat(childNames(info)).startsWith(
                "language", "category", "event", "responseType", "urgency", "severity", "certainty",
                "effective", "onset", "expires", "senderName", "headline", "description", "instruction",
                "contact");
        assertThat(text(info, "language")).isEqualTo("en");
        assertThat(text(info, "category")).isEqualTo("Met");
        assertThat(text(info, "certainty")).isEqualTo("Likely");
        assertThat(text(info, "senderName")).isEqualTo("CycloneAI screening platform");
    }

    @ParameterizedTest
    @CsvSource({
        "CRITICAL, Extreme,  Immediate",
        "HIGH,     Severe,   Immediate",
        "MEDIUM,   Moderate, Expected",
        "LOW,      Minor,    Future",
    })
    void mapsTheHighestAssessedLevelOntoSeverityAndUrgency(
            RiskLevel highest, String expectedSeverity, String expectedUrgency) throws Exception {
        Document document = parse(WRITER.write(report(highest), AdvisoryLanguage.EN, options()));

        assertThat(text(document.getDocumentElement(), "severity")).isEqualTo(expectedSeverity.strip());
        assertThat(text(document.getDocumentElement(), "urgency")).isEqualTo(expectedUrgency.strip());
    }

    @Test
    void expiresAtTheEvaluationInstantPlusTheConfiguredValidity() throws Exception {
        Document document = parse(WRITER.write(
                report(RiskLevel.HIGH),
                AdvisoryLanguage.EN,
                new CapAlertWriter.Options("ops@example.gov.in", "State EOC", Duration.ofHours(24), SENT_AT)));

        assertThat(text(document.getDocumentElement(), "effective")).isEqualTo("2023-12-05T06:00:00Z");
        assertThat(text(document.getDocumentElement(), "onset")).isEqualTo("2023-12-05T06:00:00Z");
        assertThat(text(document.getDocumentElement(), "expires")).isEqualTo("2023-12-06T06:00:00Z");
    }

    @Test
    void publishesTheStormCentreAndScreeningRadiusAsTheAlertArea() throws Exception {
        Document document = parse(WRITER.write(report(RiskLevel.HIGH), AdvisoryLanguage.EN, options()));
        Element area = (Element) document.getElementsByTagName("area").item(0);

        assertThat(text(area, "circle"))
                .as("latitude, longitude and radius in kilometres, as CAP requires")
                .isEqualTo("15.7500,80.3000 222.24");
        assertThat(text(area, "areaDesc")).contains("222 km");
    }

    @Test
    void carriesTheAssessmentNumbersAsParameters() throws Exception {
        Document document = parse(WRITER.write(report(RiskLevel.CRITICAL), AdvisoryLanguage.EN, options()));
        Element info = (Element) document.getElementsByTagName("info").item(0);

        assertThat(parameters(info))
                .containsEntry("stormId", "IO-DEMO-01")
                .containsEntry("assetsAssessed", "2")
                .containsEntry("assetsRequiringAction", "1")
                .containsEntry("intensityKt", "48")
                .containsEntry("centralPressureMb", "994")
                .containsEntry("count.CRITICAL", "1")
                .containsEntry("count.LOW", "1")
                .containsEntry("modelBasis", "75 nm exponential wind decay; no terrain, gust or quadrant modelling");
    }

    @Test
    void escapesTextSoThatAnAdvisoryFullOfMarkupStillParses() throws Exception {
        String hostile = "Warning: wind >= 50 kt & < 64 kt, \"steering\" <b>not modelled</b> — see NHC <note>";
        ImpactReport report = report(RiskLevel.HIGH, hostile);

        String document = WRITER.write(report, AdvisoryLanguage.EN, options());

        assertThat(document).doesNotContain("<b>not modelled</b>");
        assertThat(text(parse(document).getDocumentElement(), "description")).isEqualTo(hostile);
    }

    @Test
    void localisesTheTextElementsWithoutChangingTheStructure() throws Exception {
        Document hindi = parse(WRITER.write(report(RiskLevel.CRITICAL), AdvisoryLanguage.HI, options()));

        assertThat(text(hindi.getDocumentElement(), "language")).isEqualTo("hi");
        assertThat(text(hindi.getDocumentElement(), "event")).isEqualTo("उष्णकटिबंधीय चक्रवात");
        assertThat(text(hindi.getDocumentElement(), "headline")).contains("IO-DEMO-01");
        assertThat(text(hindi.getDocumentElement(), "instruction")).contains("आश्रय");
    }

    @Test
    void identifierIsStableForTheSameStormInstantAndLanguage() {
        ImpactReport report = report(RiskLevel.HIGH);

        String first = WRITER.identifier(report, AdvisoryLanguage.EN);
        String second = WRITER.identifier(report, AdvisoryLanguage.EN);
        String telugu = WRITER.identifier(report, AdvisoryLanguage.TE);

        assertThat(first).isEqualTo(second).isEqualTo("cycloneai-IO-DEMO-01-en-20231205T060000");
        assertThat(telugu)
                .as("one alert per language, so a downstream feed can de-duplicate within a language")
                .isEqualTo("cycloneai-IO-DEMO-01-te-20231205T060000");
    }

    private static CapAlertWriter.Options options() {
        return new CapAlertWriter.Options(
                "ops@example.gov.in", "CycloneAI screening platform", Duration.ofHours(24), SENT_AT);
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> childNames(Element parent) {
        NodeList children = parent.getChildNodes();
        List<String> names = new ArrayList<>();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element element) {
                names.add(element.getTagName());
            }
        }
        return names;
    }

    private static String text(Element scope, String tag) {
        NodeList nodes = scope.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private static java.util.Map<String, String> parameters(Element info) {
        java.util.Map<String, String> parameters = new java.util.HashMap<>();
        NodeList nodes = info.getElementsByTagName("parameter");
        for (int index = 0; index < nodes.getLength(); index++) {
            Element parameter = (Element) nodes.item(index);
            parameters.put(text(parameter, "valueName"), text(parameter, "value"));
        }
        return parameters;
    }

    /** An assessment shaped like the demonstration storm's. */
    private static ImpactReport report(RiskLevel highest) {
        return report(highest, "CYCLONE IMPACT ADVISORY - IO-DEMO-01\nBasis: screening model, 75 nm decay.");
    }

    private static ImpactReport report(RiskLevel highest, String advisory) {
        CycloneTrackPoint storm = new CycloneTrackPoint(
                15.75, 80.30, 48, 994, Instant.parse("2023-12-05T06:00:00Z"));

        List<AssetExposure> exposures = List.of(
                new AssetExposure(
                        new InfrastructureAsset(
                                "DEMO-SHELTER-BAPATLA",
                                "Bapatla coastal shelter",
                                AssetType.MEDICAL_SHELTER,
                                new GeoCoordinate(15.9, 80.47)),
                        13.0,
                        40,
                        highest,
                        "asset lies 13 nm from the storm centre, inside the 30 nm hurricane-force band"),
                new AssetExposure(
                        new InfrastructureAsset(
                                "DEMO-ROAD-CHENNAI",
                                "Chennai bypass (NH-32)",
                                AssetType.ARTERIAL_ROAD,
                                new GeoCoordinate(12.9, 80.15)),
                        151.0,
                        6,
                        RiskLevel.LOW,
                        "modelled wind of 6 kt at the asset and a distance of 151 nm stay below every "
                                + "gale-force threshold"));

        return new ImpactReport(
                "IO-DEMO-01",
                storm.timestamp(),
                storm,
                ImpactSummary.of(exposures),
                advisory,
                AdvisoryProvenance.deterministic(AdvisoryLanguage.EN, 3L, null),
                exposures);
    }
}
