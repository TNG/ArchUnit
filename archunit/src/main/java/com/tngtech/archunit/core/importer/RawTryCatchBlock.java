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

import com.tngtech.archunit.core.domain.JavaClassDescriptor;

class RawTryCatchBlock {
    private final Set<JavaClassDescriptor> caughtThrowables = new HashSet<>();
    private int lineNumber;
    private final Set<RawAccessRecord> accessesInTryBlock = new HashSet<>();

    Set<JavaClassDescriptor> getCaughtThrowables() {
        return caughtThrowables;
    }

    int getLineNumber() {
        return lineNumber;
    }

    Set<RawAccessRecord> getAccessesInTryBlock() {
        return accessesInTryBlock;
    }

    void addThrowable(JavaClassDescriptor throwableType) {
        caughtThrowables.add(throwableType);
    }

    void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    void addAccess(RawAccessRecord accessRecord) {
        accessesInTryBlock.add(accessRecord);
    }
}
