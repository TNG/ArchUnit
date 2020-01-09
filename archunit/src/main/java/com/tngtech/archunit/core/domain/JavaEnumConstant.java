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
import com.tngtech.archunit.core.importer.DomainBuilders.JavaEnumConstantBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class JavaEnumConstant {
    private final JavaClass declaringClass;
    private final String name;

    JavaEnumConstant(JavaEnumConstantBuilder builder) {
        this(builder.getDeclaringClass(), builder.getName());
    }

    JavaEnumConstant(JavaClass declaringClass, String name) {
        this.declaringClass = checkNotNull(declaringClass);
        this.name = checkNotNull(name);
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getDeclaringClass() {
        return declaringClass;
    }

    @PublicAPI(usage = ACCESS)
    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaEnumConstant other = (JavaEnumConstant) obj;
        return Objects.equals(this.declaringClass, other.declaringClass)
                && Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return declaringClass.getSimpleName() + "." + name;
    }
}
