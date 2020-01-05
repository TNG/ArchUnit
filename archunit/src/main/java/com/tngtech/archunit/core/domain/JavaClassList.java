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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingList;
import com.tngtech.archunit.base.Function;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class JavaClassList extends ForwardingList<JavaClass> {
    private final ImmutableList<JavaClass> elements;

    JavaClassList(List<JavaClass> elements) {
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    protected List<JavaClass> delegate() {
        return elements;
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getNames() {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (JavaClass parameter : this) {
            result.add(parameter.getName());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public static final Function<JavaClassList, List<String>> GET_NAMES = new Function<JavaClassList, List<String>>() {
        @Override
        public List<String> apply(JavaClassList input) {
            return input.getNames();
        }
    };
}