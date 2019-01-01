/*
 * Copyright 2018 TNG Technology Consulting GmbH
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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

public final class ThrowsClause implements Iterable<ThrowsDeclaration> {
    private final ImmutableList<ThrowsDeclaration> elements;

    private ThrowsClause(List<ThrowsDeclaration> elements) {
        this.elements = ImmutableList.copyOf(elements);
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getNames() {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (ThrowsDeclaration throwsDeclaration : this) {
            result.add(throwsDeclaration.getName());
        }
        return result.build();
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
        for (ThrowsDeclaration throwsDeclaration : elements) {
            if (predicate.apply(throwsDeclaration.getType())) {
                return true;
            }
        }
        return false;
    }

    @PublicAPI(usage = ACCESS)
    public int size() {
        return elements.size();
    }

    @Override
    public Iterator<ThrowsDeclaration> iterator() {
        return elements.iterator();
    }

    @PublicAPI(usage = ACCESS)
    public static final Function<ThrowsClause, List<String>> GET_NAMES = new Function<ThrowsClause, List<String>>() {
        @Override
        public List<String> apply(ThrowsClause input) {
            return input.getNames();
        }
    };

    static ThrowsClause fromThrowsDeclarations(List<ThrowsDeclaration> declarations) {
        return new ThrowsClause(declarations);
    }

    static ThrowsClause fromThrownTypes(List<JavaClass> types) {
        ImmutableList.Builder<ThrowsDeclaration> result = ImmutableList.builder();
        for (JavaClass type : types) {
            result.add(new ThrowsDeclaration(type));
        }
        return fromThrowsDeclarations(result.build());
    }
}
