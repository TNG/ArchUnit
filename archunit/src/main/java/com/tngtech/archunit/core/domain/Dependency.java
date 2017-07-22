/*
 * Copyright 2017 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

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
    static Dependency from(JavaAccess<?> access) {
        return new Dependency(access.getOriginOwner(), access.getTargetOwner(), access.getLineNumber(), access.getDescription());
    }

    static Dependency fromExtends(JavaClass origin, JavaClass targetSuperClass) {
        return createDependency(origin, targetSuperClass, "extends");
    }

    static Dependency fromImplements(JavaClass origin, JavaClass targetInterface) {
        return createDependency(origin, targetInterface, "implements");
    }

    private static Dependency createDependency(JavaClass origin, JavaClass target, String dependencyType) {
        String description = String.format("%s %s %s in %s",
                origin.getName(), dependencyType, target.getName(), Formatters.formatLocation(origin, 0));
        return new Dependency(origin, target, 0, description);
    }

    private final JavaClass originClass;
    private final JavaClass targetClass;
    private final int lineNumber;
    private final String description;

    private Dependency(JavaClass originClass, JavaClass targetClass, int lineNumber, String description) {
        this.originClass = originClass;
        this.targetClass = targetClass;
        this.lineNumber = lineNumber;
        this.description = description;
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getOriginClass() {
        return originClass;
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass getTargetClass() {
        return targetClass;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public int compareTo(Dependency o) {
        return ComparisonChain.start()
                .compare(lineNumber, o.lineNumber)
                .compare(getDescription(), o.getDescription())
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(originClass, targetClass, lineNumber, description);
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
        return Objects.equals(this.originClass, other.originClass)
                && Objects.equals(this.targetClass, other.targetClass)
                && Objects.equals(this.lineNumber, other.lineNumber)
                && Objects.equals(this.description, other.description);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("originClass", originClass)
                .add("targetClass", targetClass)
                .add("lineNumber", lineNumber)
                .add("description", description)
                .toString();
    }

    @PublicAPI(usage = ACCESS)
    public static JavaClasses toTargetClasses(Iterable<Dependency> dependencies) {
        Set<JavaClass> classes = new HashSet<>();
        for (Dependency dependency : dependencies) {
            classes.add(dependency.getTargetClass());
        }
        return JavaClasses.of(classes);
    }
}
