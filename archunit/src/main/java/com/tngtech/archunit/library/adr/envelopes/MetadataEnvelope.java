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
import com.tngtech.archunit.library.adr.Metadata;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public abstract class MetadataEnvelope implements Metadata {
    private final Metadata delegate;

    @PublicAPI(usage = ACCESS)
    public MetadataEnvelope(final Metadata delegate) {
        this.delegate = delegate;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> status() {
        return this.delegate.status();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withStatus(final String status) {
        return this.delegate.withStatus(status);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> date() {
        return this.delegate.date();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withDate(final String date) {
        return this.delegate.withDate(date);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> decisionMakers() {
        return this.delegate.decisionMakers();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withDecisionMakers(final List<String> decisionMakers) {
        return this.delegate.withDecisionMakers(decisionMakers);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> consulted() {
        return this.delegate.consulted();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withConsulted(final List<String> consulted) {
        return this.delegate.withConsulted(consulted);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> informed() {
        return this.delegate.informed();
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withInformed(final List<String> informed) {
        return this.delegate.withInformed(informed);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public String toString() {
        return this.delegate.toString();
    }
}
