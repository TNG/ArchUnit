/*
 * Copyright 2017 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.importer.DomainBuilders;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatLocation;

public abstract class JavaAccess<TARGET extends AccessTarget>
        implements HasName, HasDescription, HasOwner<JavaCodeUnit> {

    private final JavaCodeUnit origin;
    private final TARGET target;
    private final int lineNumber;
    private final int hashCode;

    JavaAccess(DomainBuilders.JavaAccessBuilder<TARGET, ?> builder) {
        this.origin = checkNotNull(builder.getOrigin());
        this.target = checkNotNull(builder.getTarget());
        this.lineNumber = builder.getLineNumber();
        this.hashCode = Objects.hash(origin.getFullName(), target.getFullName(), lineNumber);
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getOrigin() {
        return origin;
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getOriginOwner() {
        return getOrigin().getOwner();
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getTargetOwner() {
        return getTarget().getOwner();
    }

    @PublicAPI(usage = ACCESS)
    public TARGET getTarget() {
        return target;
    }

    @PublicAPI(usage = ACCESS)
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public JavaCodeUnit getOwner() {
        return getOrigin();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaAccess<?> other = (JavaAccess<?>) obj;
        return Objects.equals(this.origin.getFullName(), other.origin.getFullName()) &&
                Objects.equals(this.target.getFullName(), other.target.getFullName()) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{origin=" + origin + ", target=" + target + ", lineNumber=" + lineNumber + additionalToStringFields() + '}';
    }

    String additionalToStringFields() {
        return "";
    }

    @Override
    public String getDescription() {
        return getDescriptionWithTemplate(descriptionTemplate());
    }

    @Internal
    public String getDescriptionWithTemplate(String template) {
        String description = String.format(template, getOwner().getFullName(), getTarget().getFullName());
        String location = formatLocation(getLocationClass(), getLineNumber());
        return String.format("%s in %s", description, location);
    }

    private JavaClass getLocationClass() {
        JavaClass location = getOriginOwner();
        while (location.getEnclosingClass().isPresent()) {
            location = location.getEnclosingClass().get();
        }
        return location;
    }

    protected abstract String descriptionTemplate();

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaAccess<?>> originOwner(DescribedPredicate<? super JavaClass> predicate) {
            return origin(Get.<JavaClass>owner().is(predicate));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaAccess<?>> origin(DescribedPredicate<? super JavaCodeUnit> predicate) {
            return predicate.onResultOf(Functions.Get.origin()).as("origin " + predicate.getDescription());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaAccess<?>> originOwnerEqualsTargetOwner() {
            return new DescribedPredicate<JavaAccess<?>>("origin owner equals target owner") {
                @Override
                public boolean apply(JavaAccess<?> input) {
                    return input.getOriginOwner().equals(input.getTargetOwner());
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaAccess<?>> targetOwner(final DescribedPredicate<? super JavaClass> predicate) {
            return target(Get.<JavaClass>owner().is(predicate));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaAccess<?>> target(final DescribedPredicate<? super AccessTarget> predicate) {
            return new DescribedPredicate<JavaAccess<?>>("target " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaAccess<?> input) {
                    return predicate.apply(input.getTarget());
                }
            };
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final class Functions {
        private Functions() {
        }

        public static final class Get {
            private Get() {
            }

            @PublicAPI(usage = ACCESS)
            public static ChainableFunction<JavaAccess<?>, JavaCodeUnit> origin() {
                return new ChainableFunction<JavaAccess<?>, JavaCodeUnit>() {
                    @Override
                    public JavaCodeUnit apply(JavaAccess<?> input) {
                        return input.getOrigin();
                    }
                };
            }

            @PublicAPI(usage = ACCESS)
            public static <A extends JavaAccess<? extends T>, T extends AccessTarget> ChainableFunction<A, T> target() {
                return new ChainableFunction<A, T>() {
                    @Override
                    public T apply(A input) {
                        return input.getTarget();
                    }
                };
            }
        }
    }
}
