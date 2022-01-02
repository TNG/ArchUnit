/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents a dependency of one Java class on another Java class. Such a dependency can occur by either of the
 * following:
 * <ul>
 * <li>a class accesses a field of another class</li>
 * <li>a class calls a method/constructor of another class</li>
 * <li>a class inherits from another class</li>
 * <li>a class implements an interface</li>
 * <li>a class has a field with type of another class</li>
 * <li>a class has a method/constructor with parameter/return type of another class</li>
 * <li>a class (or method/constructor of this class) declares a type parameter referencing another class</li>
 * <li>a class (or method/constructor of this class) is annotated with an annotation of a certain type or referencing another class as annotation parameter</li>
 * <li>a method/constructor of a class references another class in a throws declaration</li>
 * <li>a class references another class object (e.g. {@code Example.class})</li>
 * <li>a class references another class in an {@code instanceof} check</li>
 * </ul>
 * Note that a {@link Dependency} will by definition never be a self-reference,
 * i.e. <code>origin</code> will never be equal to <code>target</code>.
 */
public class Dependency implements HasDescription, Comparable<Dependency>, HasSourceCodeLocation {
    private final JavaClass originClass;
    private final JavaClass targetClass;
    private final int lineNumber;
    private final String description;
    private final SourceCodeLocation sourceCodeLocation;
    private final int hashCode;

    private Dependency(JavaClass originClass, JavaClass targetClass, int lineNumber, String description) {
        checkArgument(!originClass.equals(targetClass) || targetClass.isPrimitive(),
                "Tried to create illegal dependency '%s' (%s -> %s), this is likely a bug!",
                description, originClass.getSimpleName(), targetClass.getSimpleName());

        this.originClass = originClass;
        this.targetClass = targetClass;
        this.lineNumber = lineNumber;
        this.description = description;
        this.sourceCodeLocation = SourceCodeLocation.of(originClass, lineNumber);
        hashCode = Objects.hash(originClass, targetClass, lineNumber, description);
    }

    static Set<Dependency> tryCreateFromAccess(JavaAccess<?> access) {
        JavaClass originOwner = access.getOriginOwner();
        JavaClass targetOwner = access.getTargetOwner();
        ImmutableSet.Builder<Dependency> dependencies = ImmutableSet.<Dependency>builder()
                .addAll(createComponentTypeDependencies(originOwner, access.getOrigin().getDescription(), targetOwner, access.getSourceCodeLocation()));
        dependencies.addAll(tryCreateDependency(originOwner, targetOwner, access.getDescription(), access.getLineNumber()).asSet());
        return dependencies.build();
    }

    static Dependency fromInheritance(JavaClass origin, JavaClass targetSupertype) {
        String originType = origin.isInterface() ? "Interface" : "Class";
        String originDescription = originType + " " + bracketFormat(origin.getName());

        String dependencyType = !origin.isInterface() && targetSupertype.isInterface() ? "implements" : "extends";

        String targetType = targetSupertype.isInterface() ? "interface" : "class";
        String targetDescription = bracketFormat(targetSupertype.getName());

        String dependencyDescription = originDescription + " " + dependencyType + " " + targetType + " " + targetDescription;

        String description = dependencyDescription + " in " + origin.getSourceCodeLocation();
        Optional<Dependency> result = tryCreateDependency(origin, targetSupertype, description, 0);

        if (!result.isPresent()) {
            throw new IllegalStateException(String.format("Tried to create illegal inheritance dependency '%s' (%s -> %s), this is likely a bug!",
                    description, origin.getSimpleName(), targetSupertype.getSimpleName()));
        }
        return result.get();
    }

    static Set<Dependency> tryCreateFromField(JavaField field) {
        return tryCreateDependency(field, "has type", field.getRawType());
    }

    static Set<Dependency> tryCreateFromReturnType(JavaMethod method) {
        return tryCreateDependency(method, "has return type", method.getRawReturnType());
    }

    static Set<Dependency> tryCreateFromParameter(JavaCodeUnit codeUnit, JavaClass parameter) {
        return tryCreateDependency(codeUnit, "has parameter of type", parameter);
    }

    static Set<Dependency> tryCreateFromThrowsDeclaration(ThrowsDeclaration<? extends JavaCodeUnit> declaration) {
        return tryCreateDependency(declaration.getLocation(), "throws type", declaration.getRawType());
    }

    static Set<Dependency> tryCreateFromInstanceofCheck(InstanceofCheck instanceofCheck) {
        return tryCreateDependency(
                instanceofCheck.getOwner(), "checks instanceof",
                instanceofCheck.getRawType(), instanceofCheck.getSourceCodeLocation());
    }

    static Set<Dependency> tryCreateFromReferencedClassObject(ReferencedClassObject referencedClassObject) {
        return tryCreateDependency(
                referencedClassObject.getOwner(), "references class object",
                referencedClassObject.getRawType(), referencedClassObject.getSourceCodeLocation());
    }

    static Set<Dependency> tryCreateFromAnnotation(JavaAnnotation<?> target) {
        Origin origin = findSuitableOrigin(target, target.getAnnotatedElement());
        return tryCreateDependency(origin, "is annotated with", target.getRawType());
    }

    static Set<Dependency> tryCreateFromAnnotationMember(JavaAnnotation<?> annotation, JavaClass memberType) {
        Origin origin = findSuitableOrigin(annotation, annotation.getAnnotatedElement());
        return tryCreateDependency(origin, "has annotation member of type", memberType);
    }

    static Set<Dependency> tryCreateFromTypeParameter(JavaTypeVariable<?> typeParameter, JavaClass typeParameterDependency) {
        String dependencyType = "has type parameter '" + typeParameter.getName() + "' depending on";
        Origin origin = findSuitableOrigin(typeParameter, typeParameter.getOwner());
        return tryCreateDependency(origin, dependencyType, typeParameterDependency);
    }

    static Set<Dependency> tryCreateFromGenericSuperclassTypeArguments(JavaClass originClass, JavaType superclass, JavaClass typeArgumentDependency) {
        return tryCreateDependency(originClass, genericDependencyType("superclass", superclass), typeArgumentDependency);
    }

    static Set<Dependency> tryCreateFromGenericInterfaceTypeArgument(JavaClass originClass, JavaType genericInterface, JavaClass typeArgumentDependency) {
        return tryCreateDependency(originClass, genericDependencyType("interface", genericInterface), typeArgumentDependency);
    }

    static Set<Dependency> tryCreateFromGenericFieldTypeArgument(JavaField origin, JavaClass typeArgumentDependency) {
        return tryCreateDependency(origin, genericDependencyType("type", origin.getType()), typeArgumentDependency);
    }

    static Set<Dependency> tryCreateFromGenericMethodReturnTypeArgument(JavaMethod origin, JavaClass typeArgumentDependency) {
        return tryCreateDependency(origin, genericDependencyType("return type", origin.getReturnType()), typeArgumentDependency);
    }

    static Set<Dependency> tryCreateFromGenericCodeUnitParameterTypeArgument(JavaCodeUnit origin, JavaType parameterType, JavaClass typeArgumentDependency) {
        return tryCreateDependency(origin, genericDependencyType("parameter type", parameterType), typeArgumentDependency);
    }

    private static String genericDependencyType(String genericTypeDescription, JavaType genericType) {
        return "has generic " + genericTypeDescription + " " + bracketFormat(genericType.getName()) + " with type argument depending on";
    }

    private static Origin findSuitableOrigin(Object dependencyCause, Object originCandidate) {
        if (originCandidate instanceof JavaMember) {
            JavaMember member = (JavaMember) originCandidate;
            return new Origin(member.getOwner(), member.getDescription());
        }
        if (originCandidate instanceof JavaClass) {
            JavaClass clazz = (JavaClass) originCandidate;
            return new Origin(clazz, clazz.getDescription());
        }
        if (originCandidate instanceof JavaParameter) {
            JavaParameter parameter = (JavaParameter) originCandidate;
            return new Origin(parameter.getOwner().getOwner(), parameter.getDescription());
        }
        throw new IllegalStateException("Could not find suitable dependency origin for " + dependencyCause);
    }

    private static Set<Dependency> tryCreateDependency(JavaClass origin, String dependencyType, JavaClass targetClass) {
        return tryCreateDependency(origin, origin.getDescription(), dependencyType, targetClass, origin.getSourceCodeLocation());
    }

    private static <T extends HasOwner<JavaClass> & HasDescription> Set<Dependency> tryCreateDependency(
            T origin, String dependencyType, JavaClass targetClass) {

        return tryCreateDependency(origin, dependencyType, targetClass, origin.getOwner().getSourceCodeLocation());
    }

    private static <T extends HasOwner<JavaClass> & HasDescription> Set<Dependency> tryCreateDependency(
            T origin, String dependencyType, JavaClass targetClass, SourceCodeLocation sourceCodeLocation) {

        return tryCreateDependency(origin.getOwner(), origin.getDescription(), dependencyType, targetClass, sourceCodeLocation);
    }

    private static Set<Dependency> tryCreateDependency(
            JavaClass originClass, String originDescription, String dependencyType, JavaClass targetClass, SourceCodeLocation sourceCodeLocation) {

        ImmutableSet.Builder<Dependency> dependencies = ImmutableSet.<Dependency>builder()
                .addAll(createComponentTypeDependencies(originClass, originDescription, targetClass, sourceCodeLocation));
        String targetDescription = bracketFormat(targetClass.getName());
        String dependencyDescription = originDescription + " " + dependencyType + " " + targetDescription;
        String description = dependencyDescription + " in " + sourceCodeLocation;
        dependencies.addAll(tryCreateDependency(originClass, targetClass, description, sourceCodeLocation.getLineNumber()).asSet());
        return dependencies.build();
    }

    private static Set<Dependency> createComponentTypeDependencies(
            JavaClass originClass, String originDescription, JavaClass targetClass, SourceCodeLocation sourceCodeLocation) {

        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        Optional<JavaClass> componentType = targetClass.tryGetComponentType();
        while (componentType.isPresent()) {
            String componentTypeTargetDescription = bracketFormat(componentType.get().getName());
            String componentTypeDependencyDescription = originDescription + " depends on component type " + componentTypeTargetDescription;
            String componentTypeDescription = componentTypeDependencyDescription + " in " + sourceCodeLocation;
            result.addAll(tryCreateDependency(originClass, componentType.get(), componentTypeDescription, sourceCodeLocation.getLineNumber()).asSet());
            componentType = componentType.get().tryGetComponentType();
        }
        return result.build();
    }

    private static Optional<Dependency> tryCreateDependency(JavaClass originClass, JavaClass targetClass, String description, int lineNumber) {
        if (originClass.equals(targetClass) || targetClass.isPrimitive()) {
            return Optional.empty();
        }
        return Optional.of(new Dependency(originClass, targetClass, lineNumber, description));
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
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return description;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
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
        return hashCode;
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

    private static class Origin implements HasOwner<JavaClass>, HasDescription {
        private final JavaClass originClass;
        private final String originDescription;

        private Origin(JavaClass originClass, String originDescription) {
            this.originClass = originClass;
            this.originDescription = originDescription;
        }

        @Override
        public JavaClass getOwner() {
            return originClass;
        }

        @Override
        public String getDescription() {
            return originDescription;
        }
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
