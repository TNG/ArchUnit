/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import java.util.Objects;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatLocation;

/**
 * Occurrence of an ArchUnit domain object. I.e. the location in the source code this domain object
 * is associated with.
 * <br><br>
 * E.g. considering a {@link JavaAccess} from <code>com.myapp.MyClass</code> line number 28
 * to another class <code>com.any.OtherClass</code>. Then the {@link Occurrence} would be
 * <pre><code>com.myapp.MyClass.java:28</code></pre>
 * <br>
 * An {@link Occurrence} will always only be as precise as possible from the point of Java bytecode. E.g. a field
 * of class <code>com.myapp.MyClass</code> would always be
 * <pre><code>com.myapp.MyClass.java:0</code></pre>
 * since there is no way to precisely determine the line number of a {@link JavaField} from bytecode.
 */
@PublicAPI(usage = ACCESS)
public final class Occurrence {
    private final JavaClass sourceClass;
    private final int lineNumber;
    private final String description;

    Occurrence(JavaClass sourceClass) {
        this(sourceClass, 0);
    }

    Occurrence(JavaClass sourceClass, int lineNumber) {
        this.sourceClass = checkNotNull(sourceClass);
        this.lineNumber = lineNumber;
        checkArgument(lineNumber >= 0, "Line number must be non-negative but was " + lineNumber);
        description = formatLocation(sourceClass, lineNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceClass, lineNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Occurrence other = (Occurrence) obj;
        return Objects.equals(this.sourceClass, other.sourceClass)
                && Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return description;
    }
}
