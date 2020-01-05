/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.dependencies;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A unique identifier of a {@link Slice}. All {@link JavaClasses} that are assigned to the same
 * {@link SliceIdentifier} are considered to belong to the same {@link Slice}.<br>
 * A {@link SliceIdentifier} consists of textual parts. Two {@link SliceIdentifier} are considered to
 * be equal if and only if their parts are equal. The parts can also be referred to from
 * {@link Slices#namingSlices(String)} via '{@code $x}' where '{@code x}' is the number of the part.
 */
public final class SliceIdentifier {
    private final List<String> parts;

    private SliceIdentifier(List<String> parts) {
        this.parts = ImmutableList.copyOf(parts);
    }

    List<String> getParts() {
        return parts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SliceIdentifier other = (SliceIdentifier) obj;
        return Objects.equals(this.parts, other.parts);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + parts;
    }

    @PublicAPI(usage = ACCESS)
    public static SliceIdentifier of(String... parts) {
        return of(ImmutableList.copyOf(parts));
    }

    @PublicAPI(usage = ACCESS)
    public static SliceIdentifier of(List<String> parts) {
        checkNotNull(parts, "Supplied parts may not be null");
        checkArgument(!parts.isEmpty(),
                "Parts of a %s must not be empty. Use %s.ignore() to ignore a %s",
                SliceIdentifier.class.getSimpleName(), SliceIdentifier.class.getSimpleName(), JavaClass.class.getSimpleName());

        return new SliceIdentifier(parts);
    }

    @PublicAPI(usage = ACCESS)
    public static SliceIdentifier ignore() {
        return new SliceIdentifier(Collections.<String>emptyList());
    }
}
