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

import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

class RawReferencedClassObject implements RawCodeUnitDependency<JavaClassDescriptor> {
    private final CodeUnit origin;
    private final JavaClassDescriptor target;
    private final int lineNumber;
    private final boolean declaredInLambda;

    private RawReferencedClassObject(CodeUnit origin, JavaClassDescriptor target, int lineNumber, boolean declaredInLambda) {
        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
        this.declaredInLambda = declaredInLambda;
    }

    @Override
    public CodeUnit getOrigin() {
        return origin;
    }

    @Override
    public JavaClassDescriptor getTarget() {
        return target;
    }

    String getClassName() {
        return target.getFullyQualifiedClassName();
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isDeclaredInLambda() {
        return declaredInLambda;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("origin", origin)
                .add("target", target)
                .add("lineNumber", lineNumber)
                .add("declaredInLambda", declaredInLambda)
                .toString();
    }

    static class Builder implements RawCodeUnitDependency.Builder<RawReferencedClassObject, JavaClassDescriptor> {
        private CodeUnit origin;
        private JavaClassDescriptor target;
        private int lineNumber;
        private boolean declaredInLambda;

        @Override
        public Builder withOrigin(CodeUnit origin) {
            this.origin = origin;
            return this;
        }

        @Override
        public Builder withTarget(JavaClassDescriptor target) {
            this.target = target;
            return this;
        }

        @Override
        public Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        @Override
        public Builder withDeclaredInLambda(boolean declaredInLambda) {
            this.declaredInLambda = declaredInLambda;
            return this;
        }

        @Override
        public RawReferencedClassObject build() {
            return new RawReferencedClassObject(origin, target, lineNumber, declaredInLambda);
        }
    }
}
