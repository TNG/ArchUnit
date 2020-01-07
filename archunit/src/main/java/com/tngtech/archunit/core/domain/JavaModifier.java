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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import org.objectweb.asm.Opcodes;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public enum JavaModifier {
    @PublicAPI(usage = ACCESS)
    PUBLIC(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PUBLIC),
    @PublicAPI(usage = ACCESS)
    PROTECTED(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PROTECTED),
    @PublicAPI(usage = ACCESS)
    PRIVATE(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PRIVATE),
    @PublicAPI(usage = ACCESS)
    STATIC(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_STATIC),
    @PublicAPI(usage = ACCESS)
    FINAL(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_FINAL),
    @PublicAPI(usage = ACCESS)
    VOLATILE(EnumSet.of(ApplicableType.FIELD), Opcodes.ACC_VOLATILE),
    @PublicAPI(usage = ACCESS)
    TRANSIENT(EnumSet.of(ApplicableType.FIELD), Opcodes.ACC_TRANSIENT),
    @PublicAPI(usage = ACCESS)
    ENUM(EnumSet.of(ApplicableType.FIELD), Opcodes.ACC_ENUM),
    @PublicAPI(usage = ACCESS)
    ABSTRACT(EnumSet.of(ApplicableType.CLASS, ApplicableType.METHOD), Opcodes.ACC_ABSTRACT),
    @PublicAPI(usage = ACCESS)
    SYNCHRONIZED(EnumSet.of(ApplicableType.METHOD), Opcodes.ACC_SYNCHRONIZED),
    @PublicAPI(usage = ACCESS)
    NATIVE(EnumSet.of(ApplicableType.METHOD), Opcodes.ACC_NATIVE),
    @PublicAPI(usage = ACCESS)
    BRIDGE(EnumSet.of(ApplicableType.METHOD), Opcodes.ACC_BRIDGE),
    @PublicAPI(usage = ACCESS)
    SYNTHETIC(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_SYNTHETIC);

    private final Set<ApplicableType> applicableTo;
    private final int asmAccessFlag;

    JavaModifier(Set<ApplicableType> applicableTo, int asmAccessFlag) {
        this.applicableTo = Sets.immutableEnumSet(applicableTo);
        this.asmAccessFlag = asmAccessFlag;
    }

    @PublicAPI(usage = ACCESS)
    public static Set<JavaModifier> getModifiersForClass(int asmAccess) {
        return getModifiersFor(ApplicableType.CLASS, asmAccess);
    }

    @PublicAPI(usage = ACCESS)
    public static Set<JavaModifier> getModifiersForField(int asmAccess) {
        return getModifiersFor(ApplicableType.FIELD, asmAccess);
    }

    @PublicAPI(usage = ACCESS)
    public static Set<JavaModifier> getModifiersForMethod(int asmAccess) {
        return getModifiersFor(ApplicableType.METHOD, asmAccess);
    }

    private static Set<JavaModifier> getModifiersFor(ApplicableType type, int asmAccess) {
        Set<JavaModifier> result = new HashSet<>();
        for (JavaModifier modifier : JavaModifier.values()) {
            if (modifier.applicableTo.contains(type) && modifierPresent(modifier, asmAccess)) {
                result.add(modifier);
            }
        }
        return result.isEmpty() ? Collections.<JavaModifier>emptySet() : Sets.immutableEnumSet(result);
    }

    private static boolean modifierPresent(JavaModifier modifier, int asmAccess) {
        return (modifier.asmAccessFlag & asmAccess) != 0;
    }

    private enum ApplicableType {
        CLASS, METHOD, FIELD
    }
}
