package com.tngtech.archunit.library.adr;

import java.util.List;
import java.util.Optional;

/**
 * Represents an option of an ADR with its pros and cons.
 */
public interface OptionProsAndCons {
    String title();

    Optional<String> example();

    OptionProsAndCons withExample(final String example);

    Optional<String> description();

    OptionProsAndCons withDescription(final String description);

    List<String> prosAndCons();
}
