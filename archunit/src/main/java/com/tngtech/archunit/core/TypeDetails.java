package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import static com.tngtech.archunit.core.Formatters.ensureSimpleName;

public class TypeDetails {
    private String name;
    private String simpleName;
    private String javaPackage;

    private TypeDetails(String fullName) {
        this.name = fullName;
        this.simpleName = ensureSimpleName(fullName);
        this.javaPackage = fullName.replaceAll("[^.]*$", "");
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

    public static List<TypeDetails> allOf(Class<?>... types) {
        return allOf(ImmutableList.copyOf(types));
    }

    private static List<TypeDetails> allOf(Collection<Class<?>> types) {
        ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
        for (Class<?> type : types) {
            result.add(new TypeDetails(type.getName()));
        }
        return result.build();
    }

    public static TypeDetails of(String typeName) {
        return new TypeDetails(typeName);
    }
}
