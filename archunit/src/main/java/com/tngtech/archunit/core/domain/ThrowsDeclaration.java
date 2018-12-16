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

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class ThrowsDeclaration implements HasName {

    private final JavaClass type;

    ThrowsDeclaration(JavaClass type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getType() {
        return type;
    }

    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(Class<?>... throwsDeclarationTypes) {
        return namesOf(ImmutableList.copyOf(throwsDeclarationTypes));
    }

    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(List<Class<?>> throwsDeclarationTypes) {
        ArrayList<String> result = new ArrayList<>();
        for (Class<?> paramType : throwsDeclarationTypes) {
            result.add(paramType.getName());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThrowsDeclaration that = (ThrowsDeclaration) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
