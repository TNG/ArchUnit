package com.tngtech.archunit.core.domain;

import java.util.Objects;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaEnumConstantBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class JavaEnumConstant {
    private final JavaClass declaringClass;
    private final String name;

    JavaEnumConstant(JavaEnumConstantBuilder builder) {
        this.declaringClass = builder.getDeclaringClass();
        this.name = builder.getName();
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getDeclaringClass() {
        return declaringClass;
    }

    @PublicAPI(usage = ACCESS)
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
