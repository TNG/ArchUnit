package com.tngtech.archunit.core;

import java.util.Objects;

class JavaType {
    private final String typeName;

    private JavaType(String typeName) {
        this.typeName = typeName;
    }

    static JavaType fromClassName(String name) {
        return new JavaType(name.replace("/", "."));
    }

    public String getName() {
        return typeName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaType other = (JavaType) obj;
        return Objects.equals(this.typeName, other.typeName);
    }

    @Override
    public String toString() {
        return "JavaType{typeName='" + typeName + "'}";
    }
}
