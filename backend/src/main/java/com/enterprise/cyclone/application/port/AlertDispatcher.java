package com.enterprise.cyclone.application.port;

import com.enterprise.cyclone.application.AdvisoryLanguage;

/**
 * Port for pushing a published advisory beyond this console.
 *
 * <p>An advisory that only exists inside a dashboard has not warned anybody. This port is where a
 * district control room, an SMS or WhatsApp gateway, or a CAP aggregator receives the text, and it is
 * deliberately modelled as a best-effort delivery rather than a transaction: a notification channel
 * that is down must not fail the assessment that produced the advisory.
 *
 * <p>Implementations must be thread-safe, must bound their own latency, and must report failure
 * through the returned result rather than by throwing: a caller publishing an advisory needs to show
 * the operator whether it went out, not crash.
 */
public interface AlertDispatcher {

    /** Identifier of the channel, for example {@code webhook} or {@code sms}. */
    String channel();

    /**
     * Whether this deployment has a usable channel.
     *
     * <p>False is a normal state, not an error: the console runs without a messaging provider and
     * says so, instead of offering a button that silently does nothing.
     */
    boolean configured();

    /**
     * Publishes one advisory.
     *
     * @param request the advisory to publish, non-null
     * @return the delivery outcome, never null
     */
    DispatchResult dispatch(DispatchRequest request);
}
