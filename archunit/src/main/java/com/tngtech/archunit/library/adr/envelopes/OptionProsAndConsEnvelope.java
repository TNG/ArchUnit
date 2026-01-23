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
import com.tngtech.archunit.library.adr.OptionProsAndCons;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public abstract class OptionProsAndConsEnvelope implements OptionProsAndCons {
    private final OptionProsAndCons delegate;

    @PublicAPI(usage = ACCESS)
    public OptionProsAndConsEnvelope(final OptionProsAndCons delegate) {
        this.delegate = delegate;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String title() {
        return this.delegate.title();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> description() {
        return this.delegate.description();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public OptionProsAndCons withDescription(final String description) {
        return this.delegate.withDescription(description);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> example() {
        return this.delegate.example();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public OptionProsAndCons withExample(final String example) {
        return this.delegate.withExample(example);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public List<String> prosAndCons() {
        return this.delegate.prosAndCons();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String toString() {
        return this.delegate.toString();
    }
}
