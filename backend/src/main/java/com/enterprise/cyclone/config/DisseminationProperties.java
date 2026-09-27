package com.enterprise.cyclone.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for getting an advisory out of the console.
 *
 * <p>The default state is "no channel configured", which is honest rather than broken: the console
 * still generates CAP documents for download, and the dispatch action reports that no provider is
 * wired up instead of pretending to have sent something.
 *
 * @param sender CAP {@code <sender>}: the originating system's address
 * @param senderName CAP {@code <senderName>}: the issuing organisation, shown in subscribers' feeds
 * @param validity how long an alert remains valid from its evaluation instant
 * @param webhook the outbound JSON channel (SMS/WhatsApp gateway, control-room integration, or a
 *        Cloud Function that fans out to both)
 */
@ConfigurationProperties(prefix = "app.dissemination")
public record DisseminationProperties(String sender, String senderName, Duration validity, Webhook webhook) {

    /** Default issuing organisation, used when nothing is configured. */
    public static final String DEFAULT_SENDER_NAME = "CycloneAI screening platform";

    public DisseminationProperties {
        sender = sender == null || sender.isBlank() ? "cycloneai@localhost" : sender.strip();
        senderName = senderName == null || senderName.isBlank() ? DEFAULT_SENDER_NAME : senderName.strip();
        validity = validity == null || validity.isZero() || validity.isNegative() ? Duration.ofHours(24) : validity;
        webhook = webhook == null ? Webhook.disabled() : webhook;
    }

    /**
     * @param enabled whether dispatch is offered at all
     * @param url the endpoint that accepts the advisory; absent means unconfigured
     * @param token optional bearer token for the endpoint
     */
    public record Webhook(boolean enabled, String url, String token, Duration connectTimeout, Duration readTimeout) {

        static Webhook disabled() {
            return new Webhook(false, null, null, Duration.ofSeconds(2), Duration.ofSeconds(5));
        }

        public Webhook {
            url = url == null || url.isBlank() ? null : url.strip();
            token = token == null || token.isBlank() ? null : token.strip();
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
        }

        /** True when the channel can actually be called. */
        public boolean configured() {
            return enabled && url != null;
        }
    }
}
