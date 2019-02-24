/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatLocation;

/**
 * Represents a dependency of one Java class on another Java class. Such a dependency can occur by either of the
 * following:
 * <ul>
 * <li>a class accesses a field of another class</li>
 * <li>a class calls a method of another class</li>
 * <li>a class calls a constructor of another class</li>
 * <li>a class inherits from another class (which is in fact a special case of constructor call)</li>
 * <li>a class implements an interface</li>
 * <li>a class has a field with type of another class</li>
 * <li>a class has a method/constructor with parameter/return type of another class</li>
 * </ul>
 */
public class Dependency implements HasDescription, Comparable<Dependency> {
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

    static Dependency from(JavaAccess<?> access) {
        return new Dependency(access.getOriginOwner(), access.getTargetOwner(), access.getLineNumber(), access.getDescription());
    }

    static Dependency fromInheritance(JavaClass origin, JavaClass targetSuperType) {
        String originType = origin.isInterface() ? "Interface" : "Class";
        String originDescription = originType + " " + bracketFormat(origin.getName());

        String dependencyType = !origin.isInterface() && targetSuperType.isInterface() ? "implements" : "extends";

        String targetType = targetSuperType.isInterface() ? "interface" : "class";
        String targetDescription = bracketFormat(targetSuperType.getName());

        String dependencyDescription = originDescription + " " + dependencyType + " " + targetType + " " + targetDescription;

        String description = dependencyDescription + " in " + formatLocation(origin, 0);
        return new Dependency(origin, targetSuperType, 0, description);
    }

    static Dependency fromField(JavaField field) {
        return createDependencyFromJavaMember(field, "has type", field.getRawType());
    }

    static Dependency fromReturnType(JavaMethod method) {
        return createDependencyFromJavaMember(method, "has return type", method.getRawReturnType());
    }

    static Dependency fromParameter(JavaCodeUnit codeUnit, JavaClass parameter) {
        return createDependencyFromJavaMember(codeUnit, "has parameter of type", parameter);
    }

    static Dependency fromThrowsDeclaration(ThrowsDeclaration<? extends JavaCodeUnit> declaration) {
        return createDependencyFromJavaMember(declaration.getLocation(), "throws type", declaration.getRawType());
    }

    private static Dependency createDependencyFromJavaMember(JavaMember origin, String dependencyType, JavaClass target) {
        String originDescription = origin.getDescription();
        String targetDescription = bracketFormat(target.getName());
        String dependencyDescription = originDescription + " " + dependencyType + " " + targetDescription;
        String description = dependencyDescription + " in " + formatLocation(origin.getOwner(), 0);
        return new Dependency(origin.getOwner(), target, 0, description);
    }

    private static String bracketFormat(String name) {
        return "<" + name + ">";
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

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependency(Class<?> originClass, Class<?> targetClass) {
            return dependencyOrigin(originClass).and(dependencyTarget(targetClass))
                    .as("dependency %s -> %s", originClass.getName(), targetClass.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependency(String originClassName, String targetClassName) {
            return dependencyOrigin(originClassName).and(dependencyTarget(targetClassName))
                    .as("dependency %s -> %s", originClassName, targetClassName);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependency(DescribedPredicate<? super JavaClass> originPredicate,
                DescribedPredicate<? super JavaClass> targetPredicate) {
            return dependencyOrigin(originPredicate).and(dependencyTarget(targetPredicate))
                    .as("dependency %s -> %s", originPredicate.getDescription(), targetPredicate.getDescription());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyOrigin(Class<?> clazz) {
            return dependencyOrigin(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyOrigin(String className) {
            return dependencyOrigin(HasName.Predicates.name(className).as(className));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyOrigin(final DescribedPredicate<? super JavaClass> predicate) {
            return Functions.GET_ORIGIN_CLASS.is(predicate).as("origin " + predicate.getDescription());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyTarget(Class<?> clazz) {
            return dependencyTarget(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyTarget(String className) {
            return dependencyTarget(HasName.Predicates.name(className).as(className));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<Dependency> dependencyTarget(final DescribedPredicate<? super JavaClass> predicate) {
            return Functions.GET_TARGET_CLASS.is(predicate).as("target " + predicate.getDescription());
        }
    }

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<Dependency, JavaClass> GET_ORIGIN_CLASS = new ChainableFunction<Dependency, JavaClass>() {
            @Override
            public JavaClass apply(Dependency input) {
                return input.getOriginClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<Dependency, JavaClass> GET_TARGET_CLASS = new ChainableFunction<Dependency, JavaClass>() {
            @Override
            public JavaClass apply(Dependency input) {
                return input.getTargetClass();
            }
        };
    }
}
