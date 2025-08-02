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

import com.tngtech.archunit.library.adr.Metadata;

import java.util.List;
import java.util.Optional;

public final class MdMetadata implements Metadata {

    private String status;
    private String date;
    private List<String> decisionMakers;
    private List<String> consulted;
    private List<String> informed;

    @Override
    public Optional<String> status() {
        return Optional.ofNullable(this.status);
    }

    @Override
    public Metadata withStatus(final String status) {
        this.status = status;
        return this;
    }

    @Override
    public Optional<String> date() {
        return Optional.ofNullable(this.date);
    }

    @Override
    public Metadata withDate(final String date) {
        this.date = date;
        return this;
    }

    @Override
    public Optional<List<String>> decisionMakers() {
        return Optional.ofNullable(this.decisionMakers);
    }

    @Override
    public Metadata withDecisionMakers(final List<String> decisionMakers) {
        this.decisionMakers = decisionMakers;
        return this;
    }

    @Override
    public Optional<List<String>> consulted() {
        return Optional.ofNullable(this.consulted);
    }

    @Override
    public Metadata withConsulted(final List<String> consulted) {
        this.consulted = consulted;
        return this;
    }

    @Override
    public Optional<List<String>> informed() {
        return Optional.ofNullable(this.informed);
    }

    @Override
    public Metadata withInformed(final List<String> informed) {
        this.informed = informed;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("---\n");
        status().ifPresent(s -> sb.append("status: ").append(s).append("\n"));
        date().ifPresent(d -> sb.append("date: ").append(d).append("\n"));
        decisionMakers().ifPresent(d -> sb.append("decision-makers: ").append(String.join(", ", d)).append("\n"));
        consulted().ifPresent(c -> sb.append("consulted: ").append(String.join(", ", c)).append("\n"));
        informed().ifPresent(i -> sb.append("informed: ").append(String.join(", ", i)).append("\n"));
        sb.append("---");
        return sb.toString();
    }
}
