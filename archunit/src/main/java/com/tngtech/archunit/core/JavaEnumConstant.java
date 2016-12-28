package com.tngtech.archunit.core;

import java.util.Objects;

public class JavaEnumConstant {
    private final JavaClass declaringClass;
    private final String name;

    JavaEnumConstant(JavaClass declaringClass, String name) {
        this.declaringClass = declaringClass;
        this.name = name;
    }

    public JavaClass getDeclaringClass() {
        return declaringClass;
    }

    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaEnumConstant other = (JavaEnumConstant) obj;
        return Objects.equals(this.declaringClass, other.declaringClass)
                && Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return declaringClass.getSimpleName() + "." + name;
    }
}
