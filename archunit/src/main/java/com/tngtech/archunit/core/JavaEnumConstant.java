package com.tngtech.archunit.core;

import java.util.Objects;

public class JavaEnumConstant {
    private final TypeDetails type;
    private final String name;

    public JavaEnumConstant(TypeDetails type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
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
        return Objects.equals(this.type, other.type)
                && Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return type.getSimpleName() + "." + name;
    }
}
