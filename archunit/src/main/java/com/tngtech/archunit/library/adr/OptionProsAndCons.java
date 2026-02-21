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
 * Represents an option of an ADR with its pros and cons.
 */
@PublicAPI(usage = ACCESS)
public interface OptionProsAndCons {
    @PublicAPI(usage = ACCESS)
    String title();

    @PublicAPI(usage = ACCESS)
    Optional<String> description();

    @PublicAPI(usage = ACCESS)
    OptionProsAndCons withDescription(final String description);

    @PublicAPI(usage = ACCESS)
    Optional<String> example();

    @PublicAPI(usage = ACCESS)
    OptionProsAndCons withExample(final String example);

    @PublicAPI(usage = ACCESS)
    List<String> prosAndCons();
}
