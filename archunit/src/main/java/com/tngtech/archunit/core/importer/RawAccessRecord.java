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

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;

import static com.google.common.base.Preconditions.checkNotNull;

class RawAccessRecord implements RawCodeUnitDependency<RawAccessRecord.TargetInfo> {
    private final CodeUnit origin;
    private final TargetInfo target;
    private final int lineNumber;
    private final boolean declaredInLambda;

    RawAccessRecord(CodeUnit origin, TargetInfo target, int lineNumber, boolean declaredInLambda) {
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
    public TargetInfo getTarget() {
        return target;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean isDeclaredInLambda() {
        return declaredInLambda;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
    }

    private String fieldsAsString() {
        return "origin=" + origin + ", target=" + target + ", lineNumber=" + lineNumber + ", declaredInLambda=" + declaredInLambda;
    }

    interface MemberSignature {
        String getName();

        String getDescriptor();

        String getDeclaringClassName();
    }

    static class CodeUnit implements MemberSignature {
        private final String name;
        private final String descriptor;
        private final List<JavaClassDescriptor> rawParameterTypes;
        private final List<String> rawParameterTypeNames;
        private final String declaringClassName;
        private final int hashCode;

        CodeUnit(String name, String descriptor, String declaringClassName) {
            this.name = name;
            this.descriptor = descriptor;
            this.rawParameterTypes = JavaClassDescriptorImporter.importAsmMethodArgumentTypes(descriptor);
            this.rawParameterTypeNames = namesOf(rawParameterTypes);
            this.declaringClassName = declaringClassName;
            this.hashCode = Objects.hash(name, descriptor, declaringClassName);
        }

        private static List<String> namesOf(Iterable<JavaClassDescriptor> descriptors) {
            ImmutableList.Builder<String> result = ImmutableList.builder();
            for (JavaClassDescriptor descriptor : descriptors) {
                result.add(descriptor.getFullyQualifiedClassName());
            }
            return result.build();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescriptor() {
            return descriptor;
        }

        List<JavaClassDescriptor> getRawParameterTypes() {
            return rawParameterTypes;
        }

        List<String> getRawParameterTypeNames() {
            return rawParameterTypeNames;
        }

        @Override
        public String getDeclaringClassName() {
            return declaringClassName;
        }

        JavaCodeUnit resolveFrom(ImportedClasses classes) {
            for (JavaCodeUnit method : classes.getOrResolve(getDeclaringClassName()).getCodeUnits()) {
                if (is(method)) {
                    return method;
                }
            }
            throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                    " that matches supposed origin " + this);
        }

        private boolean is(JavaCodeUnit method) {
            return getName().equals(method.getName())
                    && descriptor.equals(method.getDescriptor())
                    && getDeclaringClassName().equals(method.getOwner().getName());
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
                    Objects.equals(descriptor, codeUnit.descriptor) &&
                    Objects.equals(declaringClassName, codeUnit.declaringClassName);
        }

        @Override
        public String toString() {
            return "CodeUnit{" +
                    "name='" + name + '\'' +
                    ", descriptor=" + descriptor +
                    ", declaringClassName='" + declaringClassName + '\'' +
                    '}';
        }
    }

    static final class TargetInfo implements MemberSignature {
        final JavaClassDescriptor owner;
        final String name;
        final String desc;

        private final int hashCode;

        TargetInfo(String owner, String name, String desc) {
            this.owner = JavaClassDescriptorImporter.createFromAsmObjectTypeName(owner);
            this.name = name;
            this.desc = desc;
            hashCode = Objects.hash(owner, name, desc);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescriptor() {
            return desc;
        }

        @Override
        public String getDeclaringClassName() {
            return owner.getFullyQualifiedClassName();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TargetInfo other = (TargetInfo) obj;
            return Objects.equals(this.owner, other.owner) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.desc, other.desc);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{owner='" + owner.getFullyQualifiedClassName() + "', name='" + name + "', desc='" + desc + "'}";
        }
    }

    static class Builder extends BaseBuilder<RawAccessRecord, Builder> {
        @Override
        public RawAccessRecord build() {
            return new RawAccessRecord(origin, target, lineNumber, declaredInLambda);
        }
    }

    abstract static class BaseBuilder<ACCESS extends RawAccessRecord, SELF extends BaseBuilder<ACCESS, SELF>> implements RawCodeUnitDependency.Builder<ACCESS, TargetInfo> {
        CodeUnit origin;
        TargetInfo target;
        int lineNumber = -1;
        boolean declaredInLambda = false;

        @Override
        public SELF withOrigin(CodeUnit origin) {
            this.origin = origin;
            return self();
        }

        @Override
        public SELF withTarget(TargetInfo target) {
            this.target = target;
            return self();
        }

        @Override
        public SELF withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return self();
        }

        @Override
        public SELF withDeclaredInLambda(boolean declaredInLambda) {
            this.declaredInLambda = declaredInLambda;
            return self();
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }
    }

    static class ForField extends RawAccessRecord {
        final AccessType accessType;

        private ForField(CodeUnit origin, TargetInfo target, int lineNumber, AccessType accessType, boolean declaredInLambda) {
            super(origin, target, lineNumber, declaredInLambda);
            this.accessType = accessType;
        }

        static class Builder extends BaseBuilder<RawAccessRecord.ForField, Builder> {
            private AccessType accessType;

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return this;
            }

            @Override
            public ForField build() {
                return new ForField(super.origin, super.target, super.lineNumber, accessType, declaredInLambda);
            }
        }
    }
}
