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

class RawInstanceofCheck {
    private final JavaClassDescriptor target;
    private final int lineNumber;

    private RawInstanceofCheck(JavaClassDescriptor target, int lineNumber) {
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
    }

    static RawInstanceofCheck from(JavaClassDescriptor target, int lineNumber) {
        return new RawInstanceofCheck(target, lineNumber);
    }

    JavaClassDescriptor getTarget() {
        return target;
    }

    int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("target", target)
                .add("lineNumber", lineNumber)
                .toString();
    }
}
