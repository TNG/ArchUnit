package com.tngtech.archunit.library.adr;

import java.util.List;
import java.util.Optional;

/**
 * Metadata of the ADR.
 */
public interface Metadata {
    Optional<String> status();

    Metadata withStatus(final String status);

    Optional<String> date();

    Metadata withDate(final String date);

    Optional<List<String>> decisionMakers();

    Metadata withDecisionMakers(final List<String> decisionMakers);

    Optional<List<String>> consulted();

    Metadata withConsulted(final List<String> consulted);

    Optional<List<String>> informed();

    Metadata withInformed(final List<String> informed);
}
