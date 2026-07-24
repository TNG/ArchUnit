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
package com.tngtech.archunit.library.adr.simples;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.library.adr.Adr;
import com.tngtech.archunit.library.adr.Metadata;
import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class SimpleAdr implements Adr {

    private Metadata metadata;
    private final String title;
    private final String contextAndProblemStatement;
    private List<String> decisionDrivers;
    private final List<String> consideredOptions;
    private final String decisionOutcome;
    private List<String> consequences;
    private String confirmation;
    private List<OptionProsAndCons> optionProsAndCons;
    private String moreInformation;

    @PublicAPI(usage = ACCESS)
    public SimpleAdr(final String title, final String contextAndProblemStatement, final List<String> consideredOptions, final String decisionOutcome) {
        this.title = title;
        this.contextAndProblemStatement = contextAndProblemStatement;
        this.consideredOptions = consideredOptions;
        this.decisionOutcome = decisionOutcome;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<Metadata> metadata() {
        return Optional.ofNullable(this.metadata);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withMetadata(final Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String title() {
        return this.title;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String contextAndProblemStatement() {
        return this.contextAndProblemStatement;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> decisionDrivers() {
        return Optional.ofNullable(this.decisionDrivers);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withDecisionDrivers(final List<String> decisionDrivers) {
        this.decisionDrivers = decisionDrivers;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public List<String> consideredOptions() {
        return this.consideredOptions;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String decisionOutcome() {
        return this.decisionOutcome;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> consequences() {
        return Optional.ofNullable(this.consequences);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withConsequences(final List<String> consequences) {
        this.consequences = consequences;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> confirmation() {
        return Optional.ofNullable(this.confirmation);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withConfirmation(final String confirmation) {
        this.confirmation = confirmation;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<OptionProsAndCons>> optionProsAndCons() {
        return Optional.ofNullable(this.optionProsAndCons);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withOptionProsAndCons(final List<OptionProsAndCons> optionProsAndCons) {
        this.optionProsAndCons = optionProsAndCons;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> moreInformation() {
        return Optional.ofNullable(this.moreInformation);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Adr withMoreInformation(final String moreInformation) {
        this.moreInformation = moreInformation;
        return this;
    }
}
