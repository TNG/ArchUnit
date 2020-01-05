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
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasType;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents one declared exception of a {@link ThrowsClause}. I.e. for
 * <pre><code>void someMethod() throws FirstException, SecondException {...}</code></pre>
 * there would be one {@link ThrowsDeclaration} representing <code>FirstException</code> and one representing
 * <code>SecondException</code>
 *
 * @param <LOCATION> Represents the 'location' where this {@link ThrowsDeclaration} is declared. This can be a {@link JavaCodeUnit}, i.e.
 *                   a method or constructor. It can also be a {@link CodeUnitCallTarget CodeUnitCallTarget}
 *                   where the resolution process resolved this
 *                   {@link ThrowsDeclaration} on all resolved targets.<br>
 *                   To further elaborate, suppose we have three interfaces<br>
 *                   <code>interface A { void method() throws E1, E2{...} }</code><br>
 *                   <code>interface B { void method() throws E2, E3{...} }</code><br>
 *                   <code>interface C extends A, B {}</code><br>
 *                   Since <code>C</code> can be assigned to either <code>A</code> or <code>B</code>, it follows that
 *                   the inherited method <code>C.method()</code> only declares <code>E2</code> as its only checked Exception.<br>
 *                   Thus the {@link ThrowsClause} for the call target <code>C.method()</code> would only contain
 *                   a {@link ThrowsDeclaration} with type <code>E2</code>.<br>
 *                   For further information about the resolution process of {@link AccessTarget AccessTargets} to
 *                   {@link JavaMember JavaMembers} consult the documentation at {@link AccessTarget}.
 */
public final class ThrowsDeclaration<LOCATION extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>>
        implements HasType, HasOwner<ThrowsClause<LOCATION>> {

    private final ThrowsClause<LOCATION> throwsClause;
    private final JavaClass type;

    ThrowsDeclaration(ThrowsClause<LOCATION> throwsClause, JavaClass type) {
        this.throwsClause = throwsClause;
        this.type = type;
    }

    /**
     * @return The {@link ThrowsClause} containing this declaration
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public ThrowsClause<LOCATION> getOwner() {
        return throwsClause;
    }

    @PublicAPI(usage = ACCESS)
    public ThrowsClause<LOCATION> getThrowsClause() {
        return throwsClause;
    }

    /**
     * @return The 'location' where this {@link ThrowsDeclaration} is declared. Can be either {@link JavaCodeUnit}
     *         or {@link CodeUnitCallTarget}. Compare docs at {@link ThrowsDeclaration}.
     */
    @PublicAPI(usage = ACCESS)
    public LOCATION getLocation() {
        return throwsClause.getOwner();
    }

    /**
     * @return The class that declares the {@link LOCATION} (i.e. method, constructor, method call target, constructor call target)
     * containing this {@link ThrowsDeclaration}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getDeclaringClass() {
        return getLocation().getOwner();
    }

    /**
     * @deprecated Use {@link #getRawType()} instead
     */
    @Override
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public JavaClass getType() {
        return getRawType();
    }

    /**
     * @return The type of this {@link ThrowsDeclaration}, e.g. for a method
     * <pre><code>void method() throws SomeException {...}</code></pre>
     * the {@link JavaClass} representing <code>SomeException</code> will be returned
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation(), type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ThrowsDeclaration<?> other = (ThrowsDeclaration<?>) obj;
        return Objects.equals(this.getLocation(), other.getLocation())
                && Objects.equals(this.type, other.type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{location=" + getLocation() + ", type=" + type + '}';
    }

    @PublicAPI(usage = ACCESS)
    public static final class Functions {
        private Functions() {
        }

        public static final class Get {
            private Get() {
            }

            @PublicAPI(usage = ACCESS)
            public static <T extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>>
            ChainableFunction<ThrowsDeclaration<T>, T> location() {
                return new ChainableFunction<ThrowsDeclaration<T>, T>() {
                    @Override
                    public T apply(ThrowsDeclaration<T> input) {
                        return input.getLocation();
                    }
                };
            }
        }
    }
}
