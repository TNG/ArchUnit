/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    String contextAndProblemStatement();

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
