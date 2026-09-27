package com.enterprise.cyclone.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for model-backed evidence and writing.
 *
 * <p>Externalised in full, with the safe default being "no model configured": a deployment that
 * sets nothing still assesses storms and still issues advisories, it just issues them from the
 * deterministic generator and says so. That ordering is deliberate — the failure mode of a missing
 * API key must be a working system with an honest label, never an exception.
 *
 * @param advisory which generator to use, and the model settings
 * @param gemini the Gemini client settings; ignored when no API key is present
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(Advisory advisory, Gemini gemini) {

    /** Modes for {@link Advisory#mode()}. */
    public static final String MODE_AUTO = "auto";
    public static final String MODE_GEMINI = "gemini";
    public static final String MODE_DETERMINISTIC = "deterministic";

    public AiProperties {
        advisory = advisory == null ? Advisory.defaults() : advisory;
        gemini = gemini == null ? Gemini.defaults() : gemini;
    }

    /**
     * @param mode {@code auto} uses the model when it is configured, {@code gemini} demands it,
     *        {@code deterministic} never calls it
     * @param connectTimeout bound on establishing the connection to the model
     * @param readTimeout bound on waiting for a completion; an advisory that arrives after the
     *        briefing is worthless, so this is short on purpose
     */
    public record Advisory(String mode, Duration connectTimeout, Duration readTimeout) {

        static Advisory defaults() {
            return new Advisory(MODE_AUTO, Duration.ofSeconds(2), Duration.ofSeconds(12));
        }

        public Advisory {
            mode = mode == null || mode.isBlank() ? MODE_AUTO : mode.strip().toLowerCase(Locale.ROOT);
            if (!MODE_AUTO.equals(mode) && !MODE_GEMINI.equals(mode) && !MODE_DETERMINISTIC.equals(mode)) {
                throw new IllegalArgumentException(
                        "app.ai.advisory.mode must be one of auto, gemini or deterministic but was " + mode);
            }
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(12) : readTimeout;
        }

        /** True when the model may be used (any mode other than {@code deterministic}). */
        public boolean modelRequested() {
            return !MODE_DETERMINISTIC.equals(mode);
        }

        /** True when the configuration explicitly requires the model. */
        public boolean modelRequired() {
            return MODE_GEMINI.equals(mode);
        }
    }

    /**
     * @param enabled a kill switch independent of the key, so a model can be taken out of service
     *        without editing secrets
     * @param apiKey the Gemini API key; absent means the model is not configured
     * @param model the model identifier, recorded in provenance and shown in the console
     * @param endpoint the API base URL; overridable so the same adapter can point at Vertex AI or at
     *        an internal gateway
     */
    public record Gemini(
            boolean enabled,
            String apiKey,
            String model,
            String endpoint,
            Integer maxOutputTokens,
            Double temperature) {

        /** Default endpoint: the Gemini Developer API. */
        public static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta";

        /** Default model: fast enough to sit inside a request, strong enough to write a briefing. */
        public static final String DEFAULT_MODEL = "gemini-2.5-flash";

        static Gemini defaults() {
            return new Gemini(true, null, DEFAULT_MODEL, DEFAULT_ENDPOINT, 900, 0.2d);
        }

        public Gemini {
            model = model == null || model.isBlank() ? DEFAULT_MODEL : model.strip();
            endpoint = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.strip();
            maxOutputTokens = maxOutputTokens == null || maxOutputTokens < 64 ? 900 : maxOutputTokens;
            temperature = temperature == null ? 0.2d : Math.max(0.0d, Math.min(1.0d, temperature));
            apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.strip();
        }

        /** True when the client has everything it needs to make a call. */
        public boolean configured() {
            return enabled && apiKey != null;
        }
    }
}
