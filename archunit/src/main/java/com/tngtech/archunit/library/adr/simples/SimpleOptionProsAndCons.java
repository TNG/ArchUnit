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
import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class SimpleOptionProsAndCons implements OptionProsAndCons {
    private final String title;
    private String description;
    private String example;
    private final List<String> prosAndCons;

    @PublicAPI(usage = ACCESS)
    public SimpleOptionProsAndCons(final String title, final List<String> prosAndCons) {
        this.title = title;
        this.prosAndCons = prosAndCons;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String title() {
        return this.title;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> description() {
        return Optional.ofNullable(this.description);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public OptionProsAndCons withDescription(final String description) {
        this.description = description;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> example() {
        return Optional.ofNullable(this.example);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public OptionProsAndCons withExample(final String example) {
        this.example = example;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public List<String> prosAndCons() {
        return this.prosAndCons;
    }
}
