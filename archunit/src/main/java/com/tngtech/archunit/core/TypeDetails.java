package com.tngtech.archunit.core;

import java.util.Objects;

import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.core.Formatters.ensureSimpleName;

public class TypeDetails {
    private String name;
    private String simpleName;
    private String javaPackage;

    // FIXME: TypeDetails and JavaType seems somehow similar and meanwhile pretty much just handle some naming details. Anyway, this regex mess must be cleaned one way or another
    private TypeDetails(String fullName) {
        checkArgument(fullName.matches("(\\[+L)?(\\w+\\.)*(\\w|\\$)+;?$") || fullName.matches("\\[+\\w"), "Full name %s is invalid", fullName);

        this.name = fullName;
        this.simpleName = createSimpleName(fullName);
        this.javaPackage = isArray(fullName) ? "" : fullName.replaceAll("\\.?[^.]*$", "");
    }

    private String createSimpleName(String fullName) {
        fullName = !fullName.startsWith("L") && !isArray(fullName) ? "L" + fullName + ";" : fullName;
        return ensureSimpleName(Type.getType(fullName).getClassName());
    }

    private boolean isArray(String fullName) {
        return fullName.startsWith("[");
    }

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getPackage() {
        return javaPackage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TypeDetails other = (TypeDetails) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + "}";
    }

    public static TypeDetails of(String typeName) {
        return new TypeDetails(typeName);
    }

    /**
     * Takes an 'internal' ASM object type name, i.e. the class name but with slashes instead of periods,
     * i.e. java/lang/Object (note that this is not a descriptor like Ljava/lang/Object;)
     */
    static TypeDetails fromAsmObjectType(String objectTypeName) {
        return new TypeDetails(objectTypeName.replace("/", "."));
    }
}
