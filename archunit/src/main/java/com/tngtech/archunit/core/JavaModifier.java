package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import org.objectweb.asm.Opcodes;

public enum JavaModifier {
    PUBLIC(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PUBLIC),
    PROTECTED(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PROTECTED),
    PRIVATE(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_PRIVATE),
    STATIC(EnumSet.of(ApplicableType.FIELD, ApplicableType.METHOD), Opcodes.ACC_STATIC),
    FINAL(EnumSet.allOf(ApplicableType.class), Opcodes.ACC_FINAL),
    VOLATILE(EnumSet.of(ApplicableType.FIELD), Opcodes.ACC_VOLATILE),
    TRANSIENT(EnumSet.of(ApplicableType.FIELD), Opcodes.ACC_TRANSIENT),
    ABSTRACT(EnumSet.of(ApplicableType.CLASS, ApplicableType.METHOD), Opcodes.ACC_ABSTRACT),
    SYNCHRONIZED(EnumSet.of(ApplicableType.METHOD), Opcodes.ACC_SYNCHRONIZED),
    NATIVE(EnumSet.of(ApplicableType.METHOD), Opcodes.ACC_NATIVE);

    private final Set<ApplicableType> applicableTo;
    private final int asmAccessFlag;

    JavaModifier(Set<ApplicableType> applicableTo, int asmAccessFlag) {
        this.applicableTo = Sets.immutableEnumSet(applicableTo);
        this.asmAccessFlag = asmAccessFlag;
    }

    public static Set<JavaModifier> getModifiersForClass(int asmAccess) {
        return getModifiersFor(ApplicableType.CLASS, asmAccess);
    }

    public static Set<JavaModifier> getModifiersForField(int asmAccess) {
        return getModifiersFor(ApplicableType.FIELD, asmAccess);
    }

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
