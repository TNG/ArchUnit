package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.properties.HasOwner.Predicates.With;
import org.objectweb.asm.Opcodes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.AccessTarget.FieldAccessTarget.Functions.GET_TYPE;
import static com.tngtech.archunit.core.JavaClass.Predicates.resideInPackage;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withName;

public class JavaFieldAccess extends JavaAccess<FieldAccessTarget> {
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
    protected String descriptionTemplate() {
        return MESSAGE_TEMPLATE.get(accessType);
    }

    public static String getDescriptionTemplateFor(Set<AccessType> accessTypes) {
        return accessTypes.size() == 1
                ? MESSAGE_TEMPLATE.get(getOnlyElement(accessTypes))
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

    public static class Predicates {
        public static DescribedPredicate<JavaFieldAccess> accessType(final AccessType accessType) {
            return new DescribedPredicate<JavaFieldAccess>("access type " + accessType) {
                @Override
                public boolean apply(JavaFieldAccess input) {
                    return accessType == input.getAccessType();
                }
            };
        }

        public static DescribedPredicate<JavaFieldAccess> fieldAccessTarget(Class<?> fieldOwner, String fieldName) {
            return fieldAccessTarget(fieldOwner.getName(), fieldName);
        }

        public static DescribedPredicate<JavaFieldAccess> fieldAccessTarget(String fieldOwnerName, String fieldName) {
            return fieldAccessTarget(
                    withOwnerName(fieldOwnerName).and(withName(fieldName)).as(fieldOwnerName + "." + fieldName)
            );
        }

        private static DescribedPredicate<FieldAccessTarget> withOwnerName(String fieldOwnerName) {
            return With.<JavaClass>owner(withName(fieldOwnerName)).forSubType();
        }

        public static DescribedPredicate<JavaFieldAccess> fieldAccessTarget(final DescribedPredicate<? super FieldAccessTarget> predicate) {
            return new DescribedPredicate<JavaFieldAccess>("field access target " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaFieldAccess input) {
                    return predicate.apply(input.getTarget());
                }
            };
        }

        /**
         * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
         * characters, '..' stands for any sequence of packages.
         * For further details see {@link PackageMatcher}.
         *
         * @param packageIdentifier A string representing the identifier to match packages against
         * @return A {@link DescribedPredicate} returning true iff the package of the
         * tested {@link JavaClass} matches the identifier
         */
        public static DescribedPredicate<JavaFieldAccess> targetTypeResidesInPackage(String packageIdentifier) {
            return resideInPackage(packageIdentifier)
                    .onResultOf(GET_TYPE.after(Get.<JavaFieldAccess, FieldAccessTarget>target()))
                    .as("target type resides in package '%s'", packageIdentifier)
                    .forSubType();
        }
    }
}
