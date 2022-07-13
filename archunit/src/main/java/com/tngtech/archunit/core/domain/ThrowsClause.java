/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.ForwardingList;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static java.util.stream.Collectors.toList;

public final class ThrowsClause<LOCATION extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>>
        extends ForwardingList<ThrowsDeclaration<LOCATION>>
        implements HasOwner<LOCATION> {

    private final LOCATION location;
    private final List<ThrowsDeclaration<LOCATION>> throwsDeclarations;

    private ThrowsClause(LOCATION location, List<JavaClass> thrownTypes) {
        this.location = checkNotNull(location);
        ImmutableList.Builder<ThrowsDeclaration<LOCATION>> result = ImmutableList.builder();
        for (JavaClass type : thrownTypes) {
            result.add(new ThrowsDeclaration<>(this, type));
        }
        this.throwsDeclarations = result.build();
    }

    @PublicAPI(usage = ACCESS)
    public boolean containsType(Class<?> type) {
        return containsType(type.getName());
    }

    @PublicAPI(usage = ACCESS)
    public boolean containsType(String typeName) {
        return containsType(GET_NAME.is(equalTo(typeName)));
    }

    @PublicAPI(usage = ACCESS)
    public boolean containsType(DescribedPredicate<? super JavaClass> predicate) {
        return throwsDeclarations.stream().map(ThrowsDeclaration::getRawType).anyMatch(predicate);
    }

    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getTypes() {
        return throwsDeclarations.stream().map(GET_RAW_TYPE).collect(toList());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public LOCATION getOwner() {
        return location;
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getDeclaringClass() {
        return getOwner().getOwner();
    }

    @PublicAPI(usage = ACCESS)
    public int size() {
        return throwsDeclarations.size();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Iterator<ThrowsDeclaration<LOCATION>> iterator() {
        return throwsDeclarations.iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, getTypes());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ThrowsClause<?> other = (ThrowsClause<?>) obj;
        return Objects.equals(this.location, other.location)
                && Objects.equals(this.getTypes(), other.getTypes());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{location=" + location.getFullName() + ", throwsDeclarations=" + throwsDeclarations + '}';
    }

    static <LOCATION extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>>
    ThrowsClause<LOCATION> from(LOCATION location, List<JavaClass> types) {
        return new ThrowsClause<>(location, types);
    }

    static <LOCATION extends HasParameterTypes & HasReturnType & HasName.AndFullName & CanBeAnnotated & HasOwner<JavaClass>>
    ThrowsClause<LOCATION> empty(LOCATION location) {
        return new ThrowsClause<>(location, Collections.emptyList());
    }

    @Override
    protected List<ThrowsDeclaration<LOCATION>> delegate() {
        return throwsDeclarations;
    }

    /**
     * Predefined {@link ChainableFunction functions} to transform {@link ThrowsClause}.
     */
    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<ThrowsClause<?>, List<JavaClass>> GET_TYPES = new ChainableFunction<ThrowsClause<?>, List<JavaClass>>() {
            @Override
            public List<JavaClass> apply(ThrowsClause<?> input) {
                return input.getTypes();
            }
        };
    }
}
