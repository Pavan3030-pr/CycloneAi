package com.enterprise.cyclone.config;

import com.enterprise.cyclone.application.CapAlertWriter;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Builds the publication details for a CAP alert.
 *
 * <p>Exists so that the download path and the dispatch path cannot disagree about who is issuing an
 * alert or how long it stays valid. Both are the same publication, and a subscriber that receives a
 * downloaded document and a pushed one should not be able to tell which route it came by.
 *
 * <p>{@code sent} is taken from the injected {@link Clock} at the moment of publication, because it
 * records when the warning was issued; back-dating it to the evaluation instant would overstate the
 * warning time a recipient actually had.
 */
@Component
public class CapAlertPublication {

    private final DisseminationProperties properties;
    private final Clock clock;

    public CapAlertPublication(DisseminationProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Publication options stamped with the current instant. */
    public CapAlertWriter.Options options() {
        return new CapAlertWriter.Options(
                properties.sender(),
                properties.senderName(),
                properties.validity(),
                Instant.now(clock));
    }
}
