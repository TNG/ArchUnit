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
package com.tngtech.archunit.library.adr.envelopes;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.library.adr.Adr;
import com.tngtech.archunit.library.adr.Metadata;
import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public abstract class AdrEnvelope implements Adr {
    private final Adr delegate;

    @PublicAPI(usage = ACCESS)
    public AdrEnvelope(final Adr delegate) {
        this.delegate = delegate;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<Metadata> metadata() {
        return this.delegate.metadata();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withMetadata(final Metadata metadata) {
        return this.delegate.withMetadata(metadata);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String contextAndProblemStatement() {
        return this.delegate.contextAndProblemStatement();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String title() {
        return this.delegate.title();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> decisionDrivers() {
        return this.delegate.decisionDrivers();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withDecisionDrivers(final List<String> decisionDrivers) {
        return this.delegate.withDecisionDrivers(decisionDrivers);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public List<String> consideredOptions() {
        return this.delegate.consideredOptions();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String decisionOutcome() {
        return this.delegate.decisionOutcome();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> consequences() {
        return this.delegate.consequences();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withConsequences(final List<String> consequences) {
        return this.delegate.withConsequences(consequences);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> confirmation() {
        return this.delegate.confirmation();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withConfirmation(final String confirmation) {
        return this.delegate.withConfirmation(confirmation);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<OptionProsAndCons>> optionProsAndCons() {
        return this.delegate.optionProsAndCons();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withOptionProsAndCons(final List<OptionProsAndCons> optionProsAndCons) {
        return this.delegate.withOptionProsAndCons(optionProsAndCons);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> moreInformation() {
        return this.delegate.moreInformation();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withMoreInformation(final String moreInformation) {
        return this.delegate.withMoreInformation(moreInformation);
    }
}
