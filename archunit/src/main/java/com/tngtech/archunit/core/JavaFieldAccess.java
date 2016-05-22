package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import org.objectweb.asm.Opcodes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;

public class JavaFieldAccess extends JavaAccess<JavaField> {
    private static final Map<AccessType, String> MESSAGE_TEMPLATE = ImmutableMap.of(
            GET, "Method <%s> gets field <%s>",
            SET, "Method <%s> sets field <%s>");

    private final AccessType accessType;

    JavaFieldAccess(FieldAccessRecord record) {
        super(record);
        accessType = checkNotNull(record.getAccessType());
    }

    public AccessType getAccessType() {
        return accessType;
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
        final JavaFieldAccess other = (JavaFieldAccess) obj;
        return super.equals(other) && Objects.equals(this.accessType, other.accessType);
    }

    @Override
    protected String additionalToStringFields() {
        return ", accessType=" + accessType;
    }

    @Override
    protected String descriptionTemplate() {
        return MESSAGE_TEMPLATE.get(accessType);
    }

    public static String getDescriptionTemplateFor(Set<AccessType> accessTypes) {
        return accessTypes.size() == 1
                ? MESSAGE_TEMPLATE.get(Iterables.getOnlyElement(accessTypes))
                : "Method <%s> accesses field <%s>";
    }

    public enum AccessType {
        GET(Opcodes.GETFIELD | Opcodes.GETSTATIC), SET(Opcodes.PUTFIELD | Opcodes.PUTSTATIC);

        private final int asmOpCodes;

        AccessType(int asmOpCodes) {
            this.asmOpCodes = asmOpCodes;
        }

        static AccessType forOpCode(int opCode) {
            for (AccessType accessType : values()) {
                if ((accessType.asmOpCodes & opCode) == opCode) {
                    return accessType;
                }
            }
            throw new IllegalArgumentException(
                    "There is no " + AccessType.class.getSimpleName() + " registered for OpCode <" + opCode + ">");
        }
    }
}
