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

import com.tngtech.archunit.PublicAPI;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents an Architecture Decision Record (ADR)
 * such as <a href="https://github.com/adr/madr">these templates</a>.
 */
@PublicAPI(usage = ACCESS)
public interface Adr {
    @PublicAPI(usage = ACCESS)
    Optional<Metadata> metadata();

    @PublicAPI(usage = ACCESS)
    Adr withMetadata(final Metadata metadata);

    @PublicAPI(usage = ACCESS)
    String contextAndProblemStatement();

    @PublicAPI(usage = ACCESS)
    String title();

    @PublicAPI(usage = ACCESS)
    Optional<List<String>> decisionDrivers();

    @PublicAPI(usage = ACCESS)
    Adr withDecisionDrivers(final List<String>  decisionDrivers);

    @PublicAPI(usage = ACCESS)
    List<String> consideredOptions();

    @PublicAPI(usage = ACCESS)
    String decisionOutcome();

    @PublicAPI(usage = ACCESS)
    Optional<List<String>> consequences();

    @PublicAPI(usage = ACCESS)
    Adr withConsequences(final List<String> consequences);

    @PublicAPI(usage = ACCESS)
    Optional<String> confirmation();

    @PublicAPI(usage = ACCESS)
    Adr withConfirmation(final String confirmation);

    @PublicAPI(usage = ACCESS)
    Optional<List<OptionProsAndCons>> optionProsAndCons();

    @PublicAPI(usage = ACCESS)
    Adr withOptionProsAndCons(final List<OptionProsAndCons> optionProsAndCons);

    @PublicAPI(usage = ACCESS)
    Optional<String> moreInformation();

    @PublicAPI(usage = ACCESS)
    Adr withMoreInformation(final String moreInformation);
}
