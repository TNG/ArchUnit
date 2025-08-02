package com.tngtech.archunit.library.adr;

import java.util.List;
import java.util.Optional;

/**
 * Represents an Architecture Decision Record (ADR)
 * such as <a href="https://github.com/adr/madr">these templates</a>.
 */
public interface Adr {
    Optional<Metadata> metadata();

    Adr withMetadata(final Metadata metadata);

    String title();

    Optional<List<String>> decisionDrivers();

    Adr withDecisionDrivers(final List<String>  decisionDrivers);

    List<String> consideredOptions();

    String decisionOutcome();

    Optional<List<String>> consequences();

    Adr withConsequences(final List<String> consequences);

    Optional<String> confirmation();

    Adr withConfirmation(final String confirmation);

    Optional<List<OptionProsAndCons>> optionProsAndCons();

    Adr withOptionProsAndCons(final List<OptionProsAndCons> optionProsAndCons);

    Optional<String> moreInformation();

    Adr withMoreInformation(final String moreInformation);
}
