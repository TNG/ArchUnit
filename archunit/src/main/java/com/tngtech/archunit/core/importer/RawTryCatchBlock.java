/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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

class RawTryCatchBlock {
    private final Set<JavaClassDescriptor> caughtThrowables;
    private final int lineNumber;
    private final Set<RawAccessRecord> accessesInTryBlock;
    private final CodeUnit declaringCodeUnit;

    private RawTryCatchBlock(Builder builder) {
        this.caughtThrowables = ImmutableSet.copyOf(builder.caughtThrowables);
        this.lineNumber = builder.lineNumber;
        this.accessesInTryBlock = ImmutableSet.copyOf(builder.rawAccessesContainedInTryBlock);
        this.declaringCodeUnit = checkNotNull(builder.declaringCodeUnit);
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

    @SuppressWarnings("UnusedReturnValue")
    static class Builder {
        private final Set<JavaClassDescriptor> caughtThrowables = new HashSet<>();
        private int lineNumber;
        private final Set<RawAccessRecord> rawAccessesContainedInTryBlock = new HashSet<>();
        private CodeUnit declaringCodeUnit;

        Builder addCaughtThrowable(JavaClassDescriptor throwableType) {
            caughtThrowables.add(throwableType);
            return this;
        }

        Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
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

        RawTryCatchBlock build() {
            return new RawTryCatchBlock(this);
        }
    }
}
