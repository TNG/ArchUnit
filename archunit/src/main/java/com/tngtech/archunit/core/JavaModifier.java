package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public enum JavaModifier {
    PUBLIC(Opcodes.ACC_PUBLIC),
    PROTECTED(Opcodes.ACC_PROTECTED),
    PRIVATE(Opcodes.ACC_PRIVATE),
    STATIC(Opcodes.ACC_STATIC),
    FINAL(Opcodes.ACC_FINAL),
    VOLATILE(Opcodes.ACC_VOLATILE),
    TRANSIENT(Opcodes.ACC_TRANSIENT),
    ABSTRACT(Opcodes.ACC_ABSTRACT);

    private final int asmAccessFlag;

    JavaModifier(int asmAccessFlag) {
        this.asmAccessFlag = asmAccessFlag;
    }

    static Set<JavaModifier> getModifiersFor(int asmAccess) {
        Set<JavaModifier> result = new HashSet<>();
        for (JavaModifier modifier : JavaModifier.values()) {
            if ((modifier.asmAccessFlag & asmAccess) != 0) {
                result.add(modifier);
            }
        }
        return result.isEmpty() ? Collections.<JavaModifier>emptySet() : EnumSet.copyOf(result);
    }
}
