/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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

import java.util.List;
import java.util.Objects;

import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

class RawAccessRecord {
    final CodeUnit caller;
    final TargetInfo target;
    final int lineNumber;

    RawAccessRecord(CodeUnit caller, TargetInfo target, int lineNumber) {
        this.caller = checkNotNull(caller);
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, target, lineNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RawAccessRecord other = (RawAccessRecord) obj;
        return Objects.equals(this.caller, other.caller) &&
                Objects.equals(this.target, other.target) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
    }

    private String fieldsAsString() {
        return "caller=" + caller + ", target=" + target + ", lineNumber=" + lineNumber;
    }

    static class CodeUnit {
        private final String name;
        private final List<String> parameters;
        private final String declaringClassName;
        private final int hashCode;

        CodeUnit(String name, List<String> parameters, String declaringClassName) {
            this.name = name;
            this.parameters = parameters;
            this.declaringClassName = declaringClassName;
            this.hashCode = Objects.hash(name, parameters, declaringClassName);
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        String getDeclaringClassName() {
            return declaringClassName;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CodeUnit codeUnit = (CodeUnit) o;
            return Objects.equals(name, codeUnit.name) &&
                    Objects.equals(parameters, codeUnit.parameters) &&
                    Objects.equals(declaringClassName, codeUnit.declaringClassName);
        }

        @Override
        public String toString() {
            return "CodeUnit{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", declaringClassName='" + declaringClassName + '\'' +
                    '}';
        }

        public boolean is(JavaCodeUnit method) {
            return getName().equals(method.getName())
                    && getParameters().equals(namesOf(method.getRawParameterTypes()))
                    && getDeclaringClassName().equals(method.getOwner().getName());
        }
    }

    static class TargetInfo {
        final JavaClassDescriptor owner;
        final String name;
        final String desc;

        TargetInfo(String owner, String name, String desc) {
            this.owner = JavaClassDescriptorImporter.createFromAsmObjectTypeName(owner);
            this.name = name;
            this.desc = desc;
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name, desc);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final TargetInfo other = (TargetInfo) obj;
            return Objects.equals(this.owner, other.owner) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.desc, other.desc);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{owner='" + owner.getFullyQualifiedClassName() + "', name='" + name + "', desc='" + desc + "'}";
        }
    }

    static class Builder extends BaseBuilder<Builder> {
    }

    static class BaseBuilder<SELF extends BaseBuilder<SELF>> {
        CodeUnit caller;
        TargetInfo target;
        int lineNumber = -1;

        SELF withCaller(CodeUnit caller) {
            this.caller = caller;
            return self();
        }

        SELF withTarget(TargetInfo target) {
            this.target = target;
            return self();
        }

        SELF withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return self();
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        RawAccessRecord build() {
            return new RawAccessRecord(caller, target, lineNumber);
        }
    }

    static class ForField extends RawAccessRecord {
        final AccessType accessType;

        private ForField(CodeUnit caller, TargetInfo target, int lineNumber, AccessType accessType) {
            super(caller, target, lineNumber);
            this.accessType = accessType;
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(accessType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final ForField other = (ForField) obj;
            return Objects.equals(this.accessType, other.accessType);
        }

        static class Builder extends BaseBuilder<Builder> {
            private AccessType accessType;

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return this;
            }

            @Override
            ForField build() {
                return new ForField(super.caller, super.target, super.lineNumber, accessType);
            }
        }
    }
}
