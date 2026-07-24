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
 * Metadata of the ADR.
 */
@PublicAPI(usage = ACCESS)
public interface Metadata {
    @PublicAPI(usage = ACCESS)
    Optional<String> status();

    @PublicAPI(usage = ACCESS)
    Metadata withStatus(final String status);

    @PublicAPI(usage = ACCESS)
    Optional<String> date();

    @PublicAPI(usage = ACCESS)
    Metadata withDate(final String date);

    @PublicAPI(usage = ACCESS)
    Optional<List<String>> decisionMakers();

    @PublicAPI(usage = ACCESS)
    Metadata withDecisionMakers(final List<String> decisionMakers);

    @PublicAPI(usage = ACCESS)
    Optional<List<String>> consulted();

    @PublicAPI(usage = ACCESS)
    Metadata withConsulted(final List<String> consulted);

    @PublicAPI(usage = ACCESS)
    Optional<List<String>> informed();

    @PublicAPI(usage = ACCESS)
    Metadata withInformed(final List<String> informed);
}
