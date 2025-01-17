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
package com.tngtech.archunit.core.importer;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

class RawTryCatchBlock implements HasRawCodeUnitOrigin {
    private final Set<JavaClassDescriptor> caughtThrowables;
    private final int lineNumber;
    private final Set<RawAccessRecord> accessesInTryBlock;
    private final CodeUnit declaringCodeUnit;
    private final boolean declaredInLambda;

    private RawTryCatchBlock(Builder builder) {
        this.caughtThrowables = ImmutableSet.copyOf(builder.caughtThrowables);
        this.lineNumber = builder.lineNumber;
        this.accessesInTryBlock = ImmutableSet.copyOf(builder.rawAccessesContainedInTryBlock);
        this.declaringCodeUnit = checkNotNull(builder.declaringCodeUnit);
        this.declaredInLambda = builder.declaredInLambda;
    }

    Set<JavaClassDescriptor> getCaughtThrowables() {
        return caughtThrowables;
    }

    int getLineNumber() {
        return lineNumber;
    }

    Set<RawAccessRecord> getAccessesInTryBlock() {
        return accessesInTryBlock;
    }

    CodeUnit getDeclaringCodeUnit() {
        return declaringCodeUnit;
    }

    @Override
    public CodeUnit getOrigin() {
        return getDeclaringCodeUnit();
    }

    @Override
    public boolean isDeclaredInLambda() {
        return declaredInLambda;
    }

    static class Builder implements HasRawCodeUnitOrigin.Builder<RawTryCatchBlock> {
        private Set<JavaClassDescriptor> caughtThrowables = new HashSet<>();
        private int lineNumber;
        private Set<RawAccessRecord> rawAccessesContainedInTryBlock = new HashSet<>();
        private CodeUnit declaringCodeUnit;
        private boolean declaredInLambda = false;

        Builder withCaughtThrowables(Set<JavaClassDescriptor> caughtThrowables) {
            this.caughtThrowables = caughtThrowables;
            return this;
        }

        Builder addCaughtThrowable(JavaClassDescriptor throwableType) {
            caughtThrowables.add(throwableType);
            return this;
        }

        Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        Builder withRawAccessesContainedInTryBlock(Set<RawAccessRecord> accessRecords) {
            this.rawAccessesContainedInTryBlock = accessRecords;
            return this;
        }

        Builder addRawAccessContainedInTryBlock(RawAccessRecord accessRecord) {
            rawAccessesContainedInTryBlock.add(accessRecord);
            return this;
        }

        Builder withDeclaringCodeUnit(CodeUnit declaringCodeUnit) {
            this.declaringCodeUnit = declaringCodeUnit;
            return this;
        }

        @Override
        public Builder withOrigin(CodeUnit origin) {
            return withDeclaringCodeUnit(origin);
        }

        @Override
        public Builder withDeclaredInLambda(boolean declaredInLambda) {
            this.declaredInLambda = declaredInLambda;
            return this;
        }

        @Override
        public RawTryCatchBlock build() {
            return new RawTryCatchBlock(this);
        }

        static Builder from(RawTryCatchBlock tryCatchBlock) {
            return new RawTryCatchBlock.Builder()
                    .withCaughtThrowables(tryCatchBlock.getCaughtThrowables())
                    .withLineNumber(tryCatchBlock.getLineNumber())
                    .withRawAccessesContainedInTryBlock(tryCatchBlock.getAccessesInTryBlock())
                    .withDeclaringCodeUnit(tryCatchBlock.getOrigin())
                    .withDeclaredInLambda(tryCatchBlock.isDeclaredInLambda());
        }
    }
}
