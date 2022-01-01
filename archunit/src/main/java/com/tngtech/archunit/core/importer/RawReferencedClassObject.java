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
package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClassDescriptor;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

class RawReferencedClassObject {
    private final JavaClassDescriptor type;
    private final int lineNumber;

    private RawReferencedClassObject(JavaClassDescriptor type, int lineNumber) {
        this.type = checkNotNull(type);
        this.lineNumber = lineNumber;
    }

    static RawReferencedClassObject from(JavaClassDescriptor target, int lineNumber) {
        return new RawReferencedClassObject(target, lineNumber);
    }

    String getClassName() {
        return type.getFullyQualifiedClassName();
    }

    int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("lineNumber", lineNumber)
                .toString();
    }
}
