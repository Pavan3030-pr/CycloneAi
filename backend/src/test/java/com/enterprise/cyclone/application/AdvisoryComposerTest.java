package com.enterprise.cyclone.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The reliability contract of model-backed advisories.
 *
 * <p>These are the tests that decide whether a language model is allowed to affect a warning. The
 * requirement is not that the model always answers, it is that the system always produces an advisory
 * and always says who wrote it — so every failure mode here asserts both the text and the provenance.
 */
class AdvisoryComposerTest {

    private static final String MODEL = "gemini-2.5-flash";
    private static final DeterministicAdvisoryGenerator TEMPLATE = new DeterministicAdvisoryGenerator();

    @Test
    void usesTheTemplateWhenNoModelIsConfigured() {
        AdvisoryComposer composer = new AdvisoryComposer(TEMPLATE, null, false, false, MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.EN);

        assertThat(outcome.text()).startsWith("CYCLONE IMPACT ADVISORY - IO-DEMO-01");
        assertThat(outcome.provenance().generator()).isEqualTo(AdvisoryProvenance.DETERMINISTIC);
        assertThat(outcome.provenance().model()).isNull();
        assertThat(outcome.provenance().degraded()).as("no model was requested, so nothing is degraded").isFalse();
        assertThat(outcome.provenance().detail()).contains("no language model is configured");
        assertThat(composer.modelEnabled()).isFalse();
    }

    @Test
    void reportsDegradedWhenTheModelIsRequiredButNotConfigured() {
        AdvisoryComposer composer = new AdvisoryComposer(TEMPLATE, null, false, true, MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.EN);

        assertThat(outcome.text()).isNotBlank();
        assertThat(outcome.provenance().degraded())
                .as("an operator asked for model-written advisories and is not getting them")
                .isTrue();
        assertThat(outcome.provenance().detail()).contains("mode=gemini").contains("not configured");
    }

    @Test
    void usesTheModelWhenItIsConfiguredAndRecordsTheModelName() {
        AdvisoryComposer composer = new AdvisoryComposer(
                TEMPLATE, (context, language) -> "GEMINI WRITTEN ADVISORY for IO-DEMO-01", true, false, MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.TE);

        assertThat(outcome.text()).isEqualTo("GEMINI WRITTEN ADVISORY for IO-DEMO-01");
        assertThat(outcome.provenance().generator()).isEqualTo(AdvisoryProvenance.GEMINI);
        assertThat(outcome.provenance().model()).isEqualTo(MODEL);
        assertThat(outcome.provenance().language()).isEqualTo(AdvisoryLanguage.TE);
        assertThat(outcome.provenance().degraded()).isFalse();
        assertThat(outcome.provenance().latencyMillis()).isNotNegative();
        assertThat(outcome.provenance().modelGenerated()).isTrue();
    }

    @Test
    void fallsBackToTheTemplateAndRecordsTheReasonWhenTheModelFails() {
        AdvisoryComposer composer = new AdvisoryComposer(
                TEMPLATE,
                (context, language) -> {
                    throw new IllegalStateException("Gemini rejected the request with HTTP 429: quota exceeded");
                },
                true,
                false,
                MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.HI);

        assertThat(outcome.text()).as("a model failure must never suppress the warning")
                .startsWith("चक्रवात प्रभाव परामर्श - IO-DEMO-01");
        assertThat(outcome.provenance().generator()).isEqualTo(AdvisoryProvenance.DETERMINISTIC);
        assertThat(outcome.provenance().degraded()).isTrue();
        assertThat(outcome.provenance().detail())
                .contains("IllegalStateException")
                .contains("429");
        assertThat(outcome.provenance().modelGenerated()).isFalse();
    }

    @Test
    void fallsBackWhenTheModelReturnsNoText() {
        AdvisoryComposer composer = new AdvisoryComposer(TEMPLATE, (context, language) -> "  ", true, false, MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.EN);

        assertThat(outcome.text()).startsWith("CYCLONE IMPACT ADVISORY");
        assertThat(outcome.provenance().degraded()).isTrue();
        assertThat(outcome.provenance().detail()).contains("returned no text");
    }

    @Test
    void shortensAVeryLongFailureMessage() {
        String enormous = "x".repeat(1_000);
        AdvisoryComposer composer = new AdvisoryComposer(
                TEMPLATE,
                (context, language) -> {
                    throw new IllegalStateException(enormous);
                },
                true,
                false,
                MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.EN);

        assertThat(outcome.provenance().detail()).hasSizeLessThan(300).endsWith("…");
    }

    @Test
    void templateTextIsUsedWhenTheModelIsDisabledEntirely() {
        // mode=deterministic: the model is not even offered to the composer, so an air-gapped
        // deployment behaves identically to one that failed to configure a key.
        AdvisoryComposer composer = new AdvisoryComposer(TEMPLATE, null, false, false, MODEL);

        AdvisoryOutcome outcome = composer.compose(context(), AdvisoryLanguage.EN);

        assertThat(outcome.provenance().detail()).contains("no language model is configured");
        assertThat(outcome.provenance().model()).isNull();
    }

    /** The shared demonstration assessment, so every advisory test works from the same facts. */
    static AdvisoryContext context() {
        return DemoAssessmentFixture.context();
    }
}
