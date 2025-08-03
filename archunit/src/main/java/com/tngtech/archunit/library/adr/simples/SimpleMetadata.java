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
import com.tngtech.archunit.library.adr.Metadata;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class SimpleMetadata implements Metadata {

    private String status;
    private String date;
    private List<String> decisionMakers;
    private List<String> consulted;
    private List<String> informed;

    @PublicAPI(usage = ACCESS)
    public SimpleMetadata() {}

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> status() {
        return Optional.ofNullable(this.status);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withStatus(final String status) {
        this.status = status;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<String> date() {
        return Optional.ofNullable(this.date);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withDate(final String date) {
        this.date = date;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> decisionMakers() {
        return Optional.ofNullable(this.decisionMakers);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withDecisionMakers(final List<String> decisionMakers) {
        this.decisionMakers = decisionMakers;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> consulted() {
        return Optional.ofNullable(this.consulted);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withConsulted(final List<String> consulted) {
        this.consulted = consulted;
        return this;
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Optional<List<String>> informed() {
        return Optional.ofNullable(this.informed);
    }

    @PublicAPI(usage = ACCESS)
    @Override
    public Metadata withInformed(final List<String> informed) {
        this.informed = informed;
        return this;
    }
}
