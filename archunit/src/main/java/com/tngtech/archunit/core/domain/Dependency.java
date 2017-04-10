package com.tngtech.archunit.core.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ComparisonChain;
import com.tngtech.archunit.core.domain.properties.HasDescription;

/**
 * Represents a dependency of one Java class on another Java class. Such a dependency can occur by either of the
 * following:
 * <ul>
 * <li>a class accesses a field of another class</li>
 * <li>a class calls a method of another class</li>
 * <li>a class calls a constructor of another class</li>
 * <li>a class inherits from another class (which is in fact a special case of constructor call)</li>
 * </ul>
 */
public class Dependency implements HasDescription, Comparable<Dependency> {
    public static Dependency from(JavaAccess<?> access) {
        return new Dependency(access);
    }

    private final JavaAccess<?> access;

    private Dependency(JavaAccess<?> access) {
        this.access = access;
    }

    public JavaClass getTargetClass() {
        return access.getTarget().getOwner();
    }

    @Override
    public int hashCode() {
        return Objects.hash(access);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Dependency other = (Dependency) obj;
        return Objects.equals(this.access, other.access);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "access=" + access +
                "}";
    }

    @Override
    public String getDescription() {
        return access.getDescription();
    }

    @Override
    public int compareTo(Dependency o) {
        return ComparisonChain.start()
                .compare(access.getLineNumber(), o.access.getLineNumber())
                .compare(getDescription(), o.getDescription())
                .result();
    }

    public static JavaClasses toTargetClasses(Iterable<Dependency> dependencies) {
        Set<JavaClass> classes = new HashSet<>();
        for (Dependency dependency : dependencies) {
            classes.add(dependency.getTargetClass());
        }
        return JavaClasses.of(classes);
    }
}
