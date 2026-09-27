package com.enterprise.cyclone.adapter.out.ai;

import static com.enterprise.cyclone.application.DemoAssessmentFixture.context;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.enterprise.cyclone.application.AdvisoryContext;
import com.enterprise.cyclone.application.AdvisoryLanguage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * What is asked of the model, and what is accepted back.
 *
 * <p>Both halves matter for a safety-critical text. The prompt test asserts that every number the
 * model may mention is supplied and that it is told not to invent the rest — a model writing about a
 * cyclone will otherwise produce a plausible population figure or landfall time. The validation test
 * asserts that non-answers, truncated answers and answers about the wrong storm never reach an
 * operator.
 */
class GeminiAdvisoryGeneratorTest {

    private static final AdvisoryContext CONTEXT = context();

    @Test
    void promptCarriesEveryFactTheModelMayState() {
        String prompt = GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.EN);

        assertThat(prompt)
                .contains("IO-DEMO-01")
                .contains("2023-12-05 06:00 UTC")
                .contains("15.7500, 80.3000")
                .contains("48 kt")
                .contains("994 mb")
                .contains("Tropical Storm")
                .contains("Assets assessed: 2")
                .contains("Assets requiring pre-landfall action: 1")
                .contains("CRITICAL=1")
                .contains("Bapatla coastal shelter")
                .contains("13 nm, 40 kt")
                .contains("inside the 30 nm hurricane-force band");
    }

    @Test
    void promptDoesNotLeakAssetCoordinates() {
        String prompt = GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.EN);

        assertThat(prompt)
                .as("asset positions are not needed to write the advisory, so they are not sent")
                .doesNotContain("80.47")
                .doesNotContain("80.15");
        assertThat(prompt).containsOnlyOnce("15.7500, 80.3000");
    }

    @Test
    void promptForbidsInventionAndFormattingDrift() {
        String prompt = GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.EN);

        assertThat(prompt)
                .contains("Do not invent or estimate population, deaths, injuries")
                .contains("Do not claim the storm will or will not make landfall")
                .contains("no markdown")
                .contains("At most 180 words")
                .contains("End with this sentence");
    }

    @Test
    void promptAsksForTheRequestedLanguage() {
        // The instruction, the required closing sentence and the risk words are all in the target
        // language; the counts stay as enum names because they are compared against the API payload.
        assertThat(GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.HI))
                .contains("हिन्दी (hi)")
                .contains("आधार:")
                .contains("अनिश्चितता");
        assertThat(GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.TE))
                .contains("తెలుగు (te)")
                .contains("ఆధారం:");
        assertThat(GeminiAdvisoryGenerator.prompt(CONTEXT, AdvisoryLanguage.EN))
                .contains("Basis: linear interpolation");
    }

    @Test
    void acceptsAnAnswerThatNamesTheStormAndIsSubstantial() {
        String answer = "IO-DEMO-01 advisory. The centre is 15.75 N, 80.30 E with 48 kt and 994 mb. "
                + "Two assets were assessed and one requires pre-landfall action.";

        assertThat(GeminiAdvisoryGenerator.validate(answer, CONTEXT)).isEqualTo(answer);
    }

    @Test
    void stripsAMarkdownFenceTheModelWasToldNotToUse() {
        String fenced = "```text\nIO-DEMO-01: the storm centre is 48 kt at 994 mb and one asset requires action.\n```";

        assertThat(GeminiAdvisoryGenerator.validate(fenced, CONTEXT))
                .doesNotContain("```")
                .startsWith("IO-DEMO-01");
    }

    @Test
    void rejectsEmptyOutput() {
        assertThatIllegalStateException()
                .isThrownBy(() -> GeminiAdvisoryGenerator.validate("   ", CONTEXT))
                .withMessageContaining("no text");
    }

    @Test
    void rejectsAStubTooShortToBeAnAdvisory() {
        assertThatIllegalStateException()
                .isThrownBy(() -> GeminiAdvisoryGenerator.validate("IO-DEMO-01", CONTEXT))
                .withMessageContaining("not an advisory");
    }

    @Test
    void rejectsAnAnswerAboutTheWrongStorm() {
        String otherStorm = "WP0726 is intensifying in the western Pacific and two assets require action "
                + "according to the screening model.";

        assertThatIllegalStateException()
                .isThrownBy(() -> GeminiAdvisoryGenerator.validate(otherStorm, CONTEXT))
                .withMessageContaining("never names storm IO-DEMO-01");
    }

    @Test
    void rejectsARunawayAnswer() {
        String runaway = "IO-DEMO-01 " + "x".repeat(4_100);

        assertThatIllegalStateException()
                .isThrownBy(() -> GeminiAdvisoryGenerator.validate(runaway, CONTEXT))
                .withMessageContaining("beyond the accepted length");
    }

    @Test
    void passesTheRenderedPromptAndNoImageryToTheClient() {
        AtomicReference<String> capturedInstruction = new AtomicReference<>();
        AtomicReference<List<URI>> capturedImagery = new AtomicReference<>();
        GeminiAdvisoryClient recorder = (instruction, imageryUris) -> {
            capturedInstruction.set(instruction);
            capturedImagery.set(new ArrayList<>(imageryUris));
            return "IO-DEMO-01: 48 kt at 994 mb, one asset requires pre-landfall action in the next window.";
        };

        String advisory = new GeminiAdvisoryGenerator(recorder).generate(CONTEXT, AdvisoryLanguage.EN);

        assertThat(advisory).startsWith("IO-DEMO-01");
        assertThat(capturedInstruction.get()).contains("ASSESSMENT FACTS").contains("IO-DEMO-01");
        assertThat(capturedImagery.get()).isEmpty();
    }

    @Test
    void propagatesAFailureSoThatTheComposerCanFallBack() {
        GeminiAdvisoryClient failing = (instruction, imageryUris) -> {
            throw new IllegalStateException("Gemini could not be reached: read timed out");
        };

        assertThatIllegalStateException()
                .isThrownBy(() -> new GeminiAdvisoryGenerator(failing).generate(CONTEXT, AdvisoryLanguage.EN))
                .withMessageContaining("read timed out");
    }
}
