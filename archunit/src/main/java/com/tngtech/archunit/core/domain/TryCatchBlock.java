/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.importer.DomainBuilders.TryCatchBlockBuilder;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

@PublicAPI(usage = ACCESS)
public final class TryCatchBlock implements HasOwner<JavaCodeUnit>, HasSourceCodeLocation {
    private final JavaCodeUnit owner;
    private final Set<JavaClass> caughtThrowables;
    private final SourceCodeLocation sourceCodeLocation;
    private final Set<JavaAccess<?>> accessesContainedInTryBlock;
    private final boolean declaredInLambda;

    TryCatchBlock(TryCatchBlockBuilder builder) {
        this.owner = checkNotNull(builder.getOwner());
        this.caughtThrowables = ImmutableSet.copyOf(builder.getCaughtThrowables());
        this.sourceCodeLocation = checkNotNull(builder.getSourceCodeLocation());
        this.accessesContainedInTryBlock = ImmutableSet.copyOf(builder.getAccessesContainedInTryBlock());
        declaredInLambda = builder.isDeclaredInLambda();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getOwner() {
        return owner;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getCaughtThrowables() {
        return caughtThrowables;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesContainedInTryBlock() {
        return accessesContainedInTryBlock;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isDeclaredInLambda() {
        return declaredInLambda;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("owner", owner.getFullName())
                .add("caughtThrowables", namesOf(caughtThrowables))
                .add("location", sourceCodeLocation)
                .toString();
    }
}
