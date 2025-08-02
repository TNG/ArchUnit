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

import java.util.List;
import java.util.Optional;

/**
 * Represents an option of an ADR with its pros and cons.
 */
public interface OptionProsAndCons {
    String title();

    Optional<String> description();

    OptionProsAndCons withDescription(final String description);

    Optional<String> example();

    OptionProsAndCons withExample(final String example);

    List<String> prosAndCons();
}
