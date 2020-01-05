/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import org.objectweb.asm.Opcodes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;

public class JavaFieldAccess extends JavaAccess<FieldAccessTarget> {
    private static final Map<AccessType, String> MESSAGE_VERB = ImmutableMap.of(
            GET, "gets",
            SET, "sets");

    private final AccessType accessType;

    JavaFieldAccess(JavaFieldAccessBuilder builder) {
        super(builder);
        accessType = checkNotNull(builder.getAccessType());
    }

    @PublicAPI(usage = ACCESS)
    public AccessType getAccessType() {
        return accessType;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + accessType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaFieldAccess other = (JavaFieldAccess) obj;
        return super.equals(other) && Objects.equals(this.accessType, other.accessType);
    }

    @Override
    protected String additionalToStringFields() {
        return ", accessType=" + accessType;
    }

    @Override
    protected String descriptionVerb() {
        return MESSAGE_VERB.get(accessType);
    }

    public enum AccessType {
        @PublicAPI(usage = ACCESS)
        GET(Opcodes.GETFIELD | Opcodes.GETSTATIC),
        @PublicAPI(usage = ACCESS)
        SET(Opcodes.PUTFIELD | Opcodes.PUTSTATIC);

        private final int asmOpCodes;

        AccessType(int asmOpCodes) {
            this.asmOpCodes = asmOpCodes;
        }

        @PublicAPI(usage = ACCESS)
        public static AccessType forOpCode(int opCode) {
            for (AccessType accessType : values()) {
                if ((accessType.asmOpCodes & opCode) == opCode) {
                    return accessType;
                }
            }
            throw new IllegalArgumentException(
                    "There is no " + AccessType.class.getSimpleName() + " registered for OpCode <" + opCode + ">");
        }
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaFieldAccess> accessType(final AccessType accessType) {
            return new AccessTypePredicate(accessType);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaFieldAccess> target(final DescribedPredicate<? super FieldAccessTarget> predicate) {
            return new TargetPredicate(predicate);
        }

        private static class AccessTypePredicate extends DescribedPredicate<JavaFieldAccess> {
            private final AccessType accessType;

            AccessTypePredicate(AccessType accessType) {
                super("access type " + accessType);
                this.accessType = accessType;
            }

            @Override
            public boolean apply(JavaFieldAccess input) {
                return accessType == input.getAccessType();
            }
        }

        private static class TargetPredicate extends DescribedPredicate<JavaFieldAccess> {
            private final DescribedPredicate<? super FieldAccessTarget> predicate;

            TargetPredicate(DescribedPredicate<? super FieldAccessTarget> predicate) {
                super("target " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(JavaFieldAccess input) {
                return predicate.apply(input.getTarget());
            }
        }
    }
}
