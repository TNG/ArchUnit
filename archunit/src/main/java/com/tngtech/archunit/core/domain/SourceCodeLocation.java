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
package com.tngtech.archunit.core.domain;

import java.util.Objects;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Location in the source code of an ArchUnit domain object.
 * <br><br>
 * Consider, e.g., a {@link JavaAccess} from <code>com.myapp.MyClass</code> line number 28
 * to another class <code>com.any.OtherClass</code>. Then the {@link SourceCodeLocation} would be
 * <pre><code>com.myapp.MyClass.java:28</code></pre>
 * <br>
 * A {@link SourceCodeLocation} will always only be as precise as possible from the point of Java bytecode.
 * A field of a class, e.g., <code>com.myapp.MyClass</code> would always give
 * <pre><code>com.myapp.MyClass.java:0</code></pre>
 * since there is no way to precisely determine the line number of a {@link JavaField} from bytecode.
 *
 * @see #toString()
 */
@PublicAPI(usage = ACCESS)
public final class SourceCodeLocation {
    private static final String LOCATION_TEMPLATE = "(%s:%d)";

    @PublicAPI(usage = ACCESS)
    public static SourceCodeLocation of(JavaClass sourceClass) {
        return new SourceCodeLocation(sourceClass, 0);
    }

    @PublicAPI(usage = ACCESS)
    public static SourceCodeLocation of(JavaClass sourceClass, int lineNumber) {
        return new SourceCodeLocation(sourceClass, lineNumber);
    }

    private static String formatLocation(JavaClass sourceClass, int lineNumber) {
        Optional<String> recordedSourceFileName = sourceClass.getSource().isPresent()
                ? sourceClass.getSource().get().getFileName()
                : Optional.<String>absent();
        String sourceFileName = recordedSourceFileName.isPresent() ? recordedSourceFileName.get() : guessSourceFileName(sourceClass);
        return String.format(LOCATION_TEMPLATE, sourceFileName, lineNumber);
    }

    private static String guessSourceFileName(JavaClass location) {
        while (location.getEnclosingClass().isPresent()) {
            location = location.getEnclosingClass().get();
        }
        return location.getSimpleName() + ".java";
    }

    private final JavaClass sourceClass;
    private final int lineNumber;
    private final String description;

    private SourceCodeLocation(JavaClass sourceClass, int lineNumber) {
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
        final SourceCodeLocation other = (SourceCodeLocation) obj;
        return Objects.equals(this.sourceClass, other.sourceClass)
                && Objects.equals(this.lineNumber, other.lineNumber);
    }

    /**
     * @return "(${sourceClass.getSimpleName()}.java:${lineNumber})".
     * This format is (at least by IntelliJ Idea) recognized as location, if it's the end of a line,
     * thus enabling IDE support to jump to a definition.
     */
    @Override
    public String toString() {
        return description;
    }
}
