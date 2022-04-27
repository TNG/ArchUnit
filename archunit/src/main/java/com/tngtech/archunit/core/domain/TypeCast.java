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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.domain.properties.HasType;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class TypeCast implements HasType, HasOwner<JavaCodeUnit>, HasSourceCodeLocation {

    private final JavaCodeUnit owner;
    private final JavaClass value;
    private final SourceCodeLocation sourceCodeLocation;

    private TypeCast(JavaCodeUnit owner, JavaClass value, int lineNumber) {
        this.owner = checkNotNull(owner);
        this.value = checkNotNull(value);
        sourceCodeLocation = SourceCodeLocation.of(owner.getOwner(), lineNumber);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawType() {
        return value;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaType getType() {
        return value;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getOwner() {
        return owner;
    }

    @Override
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("owner", owner)
                .add("target", value)
                .toString();
    }

    static TypeCast from(JavaCodeUnit owner, JavaClass target, int lineNumber) {
        return new TypeCast(owner, target, lineNumber);
    }
}
