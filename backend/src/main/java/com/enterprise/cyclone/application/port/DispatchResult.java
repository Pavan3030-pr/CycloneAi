package com.enterprise.cyclone.application.port;

import java.util.Objects;

/**
 * What happened when an advisory was pushed to a channel.
 *
 * <p>Delivered or not, with a reason either way. A notification the operator cannot verify is worse
 * than no notification, so a failure is reported as plainly as a success and is never swallowed.
 *
 * @param delivered true only when the channel acknowledged the message
 * @param channel identifier of the channel, non-blank
 * @param detail why it succeeded or failed, in operator-readable terms
 * @param latencyMillis time spent talking to the channel
 */
public record DispatchResult(boolean delivered, String channel, String detail, long latencyMillis) {

    public DispatchResult {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel must not be blank");
        }
        if (detail == null || detail.isBlank()) {
            detail = delivered ? "delivered" : "not delivered";
        }
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis must not be negative");
        }
    }

    public static DispatchResult delivered(String channel, String detail, long latencyMillis) {
        return new DispatchResult(true, channel, detail, latencyMillis);
    }

    public static DispatchResult failed(String channel, String detail, long latencyMillis) {
        return new DispatchResult(false, channel, detail, latencyMillis);
    }

    /** A stable, serialisable view for the API layer. */
    public DispatchResult withChannel(String replacementChannel) {
        return new DispatchResult(delivered, Objects.requireNonNull(replacementChannel), detail, latencyMillis);
    }
}
