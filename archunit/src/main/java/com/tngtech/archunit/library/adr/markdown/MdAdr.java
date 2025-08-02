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
package com.tngtech.archunit.library.adr.markdown;

import com.tngtech.archunit.library.adr.Adr;
import com.tngtech.archunit.library.adr.Metadata;
import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

public final class MdAdr implements Adr {

    private Metadata metadata;
    private final String title;
    private List<String> decisionDrivers;
    private final List<String> consideredOptions;
    private final String decisionOutcome;
    private List<String> consequences;
    private String confirmation;
    private List<OptionProsAndCons> optionProsAndCons;
    private String moreInformation;

    public MdAdr(final String title, final List<String> consideredOptions, final String decisionOutcome) {
        this.title = title;
        this.consideredOptions = consideredOptions;
        this.decisionOutcome = decisionOutcome;
    }

    @Override
    public Optional<Metadata> metadata() {
        return Optional.ofNullable(this.metadata);
    }

    @Override
    public Adr withMetadata(final Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Optional<List<String>> decisionDrivers() {
        return Optional.ofNullable(this.decisionDrivers);
    }

    @Override
    public Adr withDecisionDrivers(final List<String> decisionDrivers) {
        this.decisionDrivers = decisionDrivers;
        return this;
    }

    @Override
    public List<String> consideredOptions() {
        return this.consideredOptions;
    }

    @Override
    public String decisionOutcome() {
        return this.decisionOutcome;
    }

    @Override
    public Optional<List<String>> consequences() {
        return Optional.ofNullable(this.consequences);
    }

    @Override
    public Adr withConsequences(final List<String> consequences) {
        this.consequences = consequences;
        return this;
    }

    @Override
    public Optional<String> confirmation() {
        return Optional.ofNullable(this.confirmation);
    }

    @Override
    public Adr withConfirmation(final String confirmation) {
        this.confirmation = confirmation;
        return this;
    }

    @Override
    public Optional<List<OptionProsAndCons>> optionProsAndCons() {
        return Optional.ofNullable(this.optionProsAndCons);
    }

    @Override
    public Adr withOptionProsAndCons(final List<OptionProsAndCons> optionProsAndCons) {
        this.optionProsAndCons = optionProsAndCons;
        return this;
    }

    @Override
    public Optional<String> moreInformation() {
        return Optional.ofNullable(this.moreInformation);
    }

    @Override
    public Adr withMoreInformation(final String moreInformation) {
        this.moreInformation = moreInformation;
        return this;
    }
}
