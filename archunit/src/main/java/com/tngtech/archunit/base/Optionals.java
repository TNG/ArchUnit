/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.base;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.tngtech.archunit.Internal;

import static java.util.Collections.emptySet;

@Internal
public final class Optionals {
    private Optionals() {
    }

    public static <T> Set<T> asSet(Optional<T> input) {
        return input.map(Collections::singleton).orElse(emptySet());
    }

    public static <T> Stream<T> stream(Optional<T> input) {
        return input.map(Stream::of).orElse(Stream.empty());
    }
}
