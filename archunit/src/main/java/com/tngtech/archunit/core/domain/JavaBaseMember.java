/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.*;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaBaseMemberBuilder;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Abstracts over common properties of {@link JavaMember} and {@link JavaRecordComponent}.
 */
@Internal
public abstract class JavaBaseMember implements
        HasName.AndFullName, HasDescriptor, HasOwner<JavaClass>, HasSourceCodeLocation {

    private final String name;
    private final String descriptor;
    private final JavaClass owner;
    private final SourceCodeLocation sourceCodeLocation;
    private ReverseDependencies reverseDependencies = ReverseDependencies.EMPTY;

    JavaBaseMember(JavaBaseMemberBuilder<?, ?> builder) {
        this.name = checkNotNull(builder.getName());
        this.descriptor = checkNotNull(builder.getDescriptor());
        this.owner = checkNotNull(builder.getOwner());
        this.sourceCodeLocation = SourceCodeLocation.of(owner, builder.getFirstLineNumber());
    }

    /**
     * Similar to {@link JavaType#getAllInvolvedRawTypes()}, this method returns all raw types involved in this {@link JavaBaseMember member's} signature.
     * For more concrete details refer to {@link JavaField#getAllInvolvedRawTypes()} and {@link JavaCodeUnit#getAllInvolvedRawTypes()}.
     *
     * @return All raw types involved in the signature of this member
     */
    @PublicAPI(usage = ACCESS)
    public abstract Set<JavaClass> getAllInvolvedRawTypes();

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    @Override
    @Internal
    public String getDescriptor() {
        return descriptor;
    }

    @PublicAPI(usage = ACCESS)
    public abstract Set<? extends JavaAccess<?>> getAccessesToSelf();

    protected ReverseDependencies getReverseDependencies() {
        return reverseDependencies;
    }

    void setReverseDependencies(ReverseDependencies reverseDependencies) {
        this.reverseDependencies = reverseDependencies;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getFullName() + '}';
    }
}
