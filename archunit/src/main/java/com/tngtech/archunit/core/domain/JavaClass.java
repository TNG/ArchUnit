/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext.AccessContext;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RAW_RETURN_TYPE;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

public class JavaClass implements HasName.AndFullName, HasAnnotations<JavaClass>, HasModifiers, HasSourceCodeLocation {
    private final Optional<Source> source;
    private final SourceCodeLocation sourceCodeLocation;
    private final JavaType javaType;
    private JavaPackage javaPackage;
    private final boolean isInterface;
    private final boolean isEnum;
    private final Set<JavaModifier> modifiers;
    private final Supplier<Class<?>> reflectSupplier;
    private Set<JavaField> fields = new HashSet<>();
    private Set<JavaCodeUnit> codeUnits = new HashSet<>();
    private Set<JavaMethod> methods = new HashSet<>();
    private Set<JavaMember> members = new HashSet<>();
    private Set<JavaConstructor> constructors = new HashSet<>();
    private Optional<JavaStaticInitializer> staticInitializer = Optional.absent();
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> interfaces = new HashSet<>();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();
    private Optional<JavaClass> componentType = Optional.absent();
    private Supplier<Map<String, JavaAnnotation<JavaClass>>> annotations =
            Suppliers.ofInstance(Collections.<String, JavaAnnotation<JavaClass>>emptyMap());
    private Supplier<Set<JavaMethod>> allMethods;
    private Supplier<Set<JavaConstructor>> allConstructors;
    private Supplier<Set<JavaField>> allFields;
    private final Supplier<Set<JavaMember>> allMembers = Suppliers.memoize(new Supplier<Set<JavaMember>>() {
        @Override
        public Set<JavaMember> get() {
            return ImmutableSet.<JavaMember>builder()
                    .addAll(getAllFields())
                    .addAll(getAllMethods())
                    .addAll(getAllConstructors())
                    .build();
        }
    });
    private MemberDependenciesOnClass memberDependenciesOnClass;

    JavaClass(JavaClassBuilder builder) {
        source = checkNotNull(builder.getSource());
        javaType = checkNotNull(builder.getJavaType());
        isInterface = builder.isInterface();
        isEnum = builder.isEnum();
        modifiers = checkNotNull(builder.getModifiers());
        reflectSupplier = Suppliers.memoize(new ReflectClassSupplier());
        sourceCodeLocation = SourceCodeLocation.of(this);
        javaPackage = JavaPackage.simple(this);
    }

    /**
     * @return The {@link Source} of this {@link JavaClass}, i.e. where this class has been imported from
     */
    @PublicAPI(usage = ACCESS)
    public Optional<Source> getSource() {
        return source;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SourceCodeLocation getSourceCodeLocation() {
        return sourceCodeLocation;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return "Class <" + getName() + ">";
    }

    /**
     * @return The fully qualified name of this {@link JavaClass}, compare {@link Class#getName()} of the Reflection API
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return javaType.getName();
    }

    /**
     * @return The fully qualified name of this {@link JavaClass}, i.e. the result is the same as invoking {@link #getName()}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getFullName() {
        return getName();
    }

    @PublicAPI(usage = ACCESS)
    public String getSimpleName() {
        return javaType.getSimpleName();
    }

    @PublicAPI(usage = ACCESS)
    public JavaPackage getPackage() {
        return javaPackage;
    }

    void setPackage(JavaPackage javaPackage) {
        this.javaPackage = checkNotNull(javaPackage);
    }

    @PublicAPI(usage = ACCESS)
    public String getPackageName() {
        return javaType.getPackageName();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isPrimitive() {
        return javaType.isPrimitive();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isInterface() {
        return isInterface;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isEnum() {
        return isEnum;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isArray() {
        return javaType.isArray();
    }

    /**
     * This is a convenience method for {@link #tryGetComponentType()} in cases where
     * clients know that this type is certainly an array type and thus the component type present.
     * @throws IllegalArgumentException if this class is no array
     * @return The result of {@link #tryGetComponentType()}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getComponentType() {
        return tryGetComponentType().getOrThrow(new IllegalArgumentException(
                String.format("Type %s is no array", getSimpleName())));
    }

    /**
     * Returns the component type of this class, if this class is an array, otherwise
     * {@link Optional#absent()}. The component type is the type of the elements of an array type.
     * Consider {@code String[]}, then the component type would be {@code String}.
     * Likewise for {@code String[][]} the component type would be {@code String[]}.
     * @return The component type, if this type is an array, otherwise {@link Optional#absent()}
     */
    @PublicAPI(usage = ACCESS)
    Optional<JavaClass> tryGetComponentType() {
        return componentType;
    }

    /**
     * @return Returns true if this class is declared within another class.
     *         Returns false for top-level classes.
     */
    @PublicAPI(usage = ACCESS)
    public boolean isNestedClass() {
        return enclosingClass.isPresent();
    }

    /**
     * @return Returns true if this class is a non-static nested class.
     *         Returns false otherwise.
     */
    @PublicAPI(usage = ACCESS)
    public boolean isInnerClass() {
        return isNestedClass() && !getModifiers().contains(JavaModifier.STATIC);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.get().containsKey(annotationTypeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.get().values(), predicate);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> type) {
        return isMetaAnnotatedWith(type.getName());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(String typeName) {
        return isMetaAnnotatedWith(GET_RAW_TYPE.then(GET_NAME).is(equalTo(typeName)));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return CanBeAnnotated.Utils.isMetaAnnotatedWith(annotations.get().values(), predicate);
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return An {@link Annotation} of the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #tryGetAnnotationOfType(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaAnnotation<JavaClass> getAnnotationOfType(String typeName) {
        return tryGetAnnotationOfType(typeName).getOrThrow(new IllegalArgumentException(
                String.format("Type %s is not annotated with @%s", getSimpleName(), typeName)));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<JavaClass>> getAnnotations() {
        return ImmutableSet.copyOf(annotations.get().values());
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return An {@link Optional} containing an {@link Annotation} of the given annotation type,
     * if this class is annotated with the given type, otherwise Optional.absent()
     * @see #isAnnotatedWith(Class)
     * @see #getAnnotationOfType(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).transform(toAnnotationOfType(type));
    }

    /**
     * Same as {@link #tryGetAnnotationOfType(Class)}, but takes the type name.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Optional<JavaAnnotation<JavaClass>> tryGetAnnotationOfType(String typeName) {
        return Optional.fromNullable(annotations.get().get(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> getSuperClass() {
        return superClass;
    }

    /**
     * @return The complete class hierarchy, i.e. the class itself and the result of {@link #getAllSuperClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getClassHierarchy() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        result.add(this);
        result.addAll(getAllSuperClasses());
        return result.build();
    }

    /**
     * @return All super classes sorted ascending by distance in the class hierarchy, i.e. first the direct super class,
     * then the super class of the super class and so on. Includes Object.class in the result.
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getAllSuperClasses() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        JavaClass current = this;
        while (current.getSuperClass().isPresent()) {
            current = current.getSuperClass().get();
            result.add(current);
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getSubClasses() {
        return subClasses;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getInterfaces() {
        return interfaces;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllInterfaces() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaClass i : interfaces) {
            result.add(i);
            result.addAll(i.getAllInterfaces());
        }
        if (superClass.isPresent()) {
            result.addAll(superClass.get().getAllInterfaces());
        }
        return result.build();
    }

    /**
     * @return All classes, this class is assignable to, in particular
     * <ul>
     * <li>self</li>
     * <li>superclasses this class extends</li>
     * <li>interfaces this class implements</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllClassesSelfIsAssignableTo() {
        return ImmutableSet.<JavaClass>builder()
                .add(this)
                .addAll(getAllSuperClasses())
                .addAll(getAllInterfaces())
                .build();
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> getEnclosingClass() {
        return enclosingClass;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllSubClasses() {
        Set<JavaClass> result = new HashSet<>();
        for (JavaClass subClass : subClasses) {
            result.add(subClass);
            result.addAll(subClass.getAllSubClasses());
        }
        return result;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMember> getMembers() {
        return members;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMember> getAllMembers() {
        return allMembers.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getFields() {
        return fields;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getAllFields() {
        checkNotNull(allFields, "Method may not be called before construction of hierarchy is complete");
        return allFields.get();
    }

    @PublicAPI(usage = ACCESS)
    public JavaField getField(String name) {
        return tryGetField(name).getOrThrow(new IllegalArgumentException("No field with name '" + name + " in class " + getName()));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaField> tryGetField(String name) {
        for (JavaField field : fields) {
            if (name.equals(field.getName())) {
                return Optional.of(field);
            }
        }
        return Optional.absent();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnit> getCodeUnits() {
        return codeUnits;
    }

    /**
     * @param name       The name of the code unit, can be a method name, but also
     *                   {@link JavaConstructor#CONSTRUCTOR_NAME CONSTRUCTOR_NAME}
     *                   or {@link JavaStaticInitializer#STATIC_INITIALIZER_NAME STATIC_INITIALIZER_NAME}
     * @param parameters The parameter signature of the method specified as {@link Class Class} Objects
     * @return A code unit (method, constructor or static initializer) with the given signature
     */
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getCodeUnitWithParameterTypes(String name, Class<?>... parameters) {
        return getCodeUnitWithParameterTypes(name, ImmutableList.copyOf(parameters));
    }

    /**
     * Same as {@link #getCodeUnitWithParameterTypes(String, Class[])}, but with parameter signature specified as full class names
     */
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, String... parameters) {
        return getCodeUnitWithParameterTypeNames(name, ImmutableList.copyOf(parameters));
    }

    /**
     * @see #getCodeUnitWithParameterTypes(String, Class[])
     */
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getCodeUnitWithParameterTypes(String name, List<Class<?>> parameters) {
        return getCodeUnitWithParameterTypeNames(name, namesOf(parameters));
    }

    /**
     * @see #getCodeUnitWithParameterTypeNames(String, String...)
     */
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return findMatchingCodeUnit(codeUnits, name, parameters);
    }

    private <T extends JavaCodeUnit> T findMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        Optional<T> codeUnit = tryFindMatchingCodeUnit(codeUnits, name, parameters);
        if (!codeUnit.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("No code unit with name '%s' and parameters %s in codeUnits %s of class %s",
                            name, parameters, codeUnits, getName()));
        }
        return codeUnit.get();
    }

    private <T extends JavaCodeUnit> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(codeUnit.getRawParameterTypes().getNames())) {
                return Optional.of(codeUnit);
            }
        }
        return Optional.absent();
    }

    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name) {
        return findMatchingCodeUnit(methods, name, Collections.<String>emptyList());
    }

    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(methods, name, namesOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name, String... parameters) {
        return findMatchingCodeUnit(methods, name, ImmutableList.copyOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name) {
        return tryFindMatchingCodeUnit(methods, name, Collections.<String>emptyList());
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name, Class<?>... parameters) {
        return tryFindMatchingCodeUnit(methods, name, namesOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name, String... parameters) {
        return tryFindMatchingCodeUnit(methods, name, ImmutableList.copyOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethods() {
        return methods;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getAllMethods() {
        checkNotNull(allMethods, "Method may not be called before construction of hierarchy is complete");
        return allMethods.get();
    }

    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor() {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, Collections.<String>emptyList());
    }

    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, namesOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor(String... parameters) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, ImmutableList.copyOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getAllConstructors() {
        checkNotNull(allConstructors, "Method may not be called before construction of hierarchy is complete");
        return allConstructors.get();
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaStaticInitializer> getStaticInitializer() {
        return staticInitializer;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return union(getFieldAccessesFromSelf(), getCallsFromSelf());
    }

    /**
     * @return Set of all {@link JavaAccess} in the class hierarchy, as opposed to the accesses this class directly performs.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAllAccessesFromSelf() {
        ImmutableSet.Builder<JavaAccess<?>> result = ImmutableSet.builder();
        for (JavaClass clazz : getClassHierarchy()) {
            result.addAll(clazz.getAccessesFromSelf());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccessesFromSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getFieldAccesses());
        }
        return result.build();
    }

    /**
     * Returns all calls of this class to methods or constructors.
     *
     * @see #getMethodCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCallsFromSelf() {
        return union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getMethodCallsFromSelf());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getConstructorCallsFromSelf());
        }
        return result.build();
    }

    /**
     * Returns all dependencies originating directly from this class (i.e. not just from a superclass),
     * where a dependency can be
     * <ul>
     * <li>field access</li>
     * <li>method call</li>
     * <li>constructor call</li>
     * <li>extending a class</li>
     * <li>implementing an interface</li>
     * <li>referencing in throws declaration</li>
     * </ul>
     *
     * @return All dependencies originating directly from this class (i.e. where this class is the origin)
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        result.addAll(dependenciesFromAccesses(getAccessesFromSelf()));
        result.addAll(inheritanceDependenciesFromSelf());
        result.addAll(fieldDependenciesFromSelf());
        result.addAll(returnTypeDependenciesFromSelf());
        result.addAll(methodParameterDependenciesFromSelf());
        result.addAll(throwsDeclarationDependenciesFromSelf());
        result.addAll(constructorParameterDependenciesFromSelf());
        result.addAll(annotationDependenciesFromSelf());
        return result.build();
    }

    /**
     * Like {@link #getDirectDependenciesFromSelf()}, but instead returns all dependencies where this class
     * is target.
     *
     * @return Dependencies where this class is the target.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesToSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        result.addAll(dependenciesFromAccesses(getAccessesToSelf()));
        result.addAll(inheritanceDependenciesToSelf());
        result.addAll(fieldDependenciesToSelf());
        result.addAll(returnTypeDependenciesToSelf());
        result.addAll(methodParameterDependenciesToSelf());
        result.addAll(throwsDeclarationDependenciesToSelf());
        result.addAll(constructorParameterDependenciesToSelf());
        result.addAll(annotationDependenciesToSelf());
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccessesToSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            result.addAll(field.getAccessesToSelf());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsToSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaMethod method : methods) {
            result.addAll(method.getCallsOfSelf());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsToSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaConstructor constructor : constructors) {
            result.addAll(constructor.getCallsOfSelf());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesToSelf() {
        return ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getFieldAccessesToSelf())
                .addAll(getMethodCallsToSelf())
                .addAll(getConstructorCallsToSelf())
                .build();
    }

    /**
     * @return Fields of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getFieldsWithTypeOfSelf() {
        return memberDependenciesOnClass.getFieldsWithTypeOfClass();
    }

    /**
     * @return Methods of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithParameterTypeOfSelf() {
        return memberDependenciesOnClass.getMethodsWithParameterTypeOfClass();
    }

    /**
     * @return Methods of all imported classes that have a return type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithReturnTypeOfSelf() {
        return memberDependenciesOnClass.getMethodsWithReturnTypeOfClass();
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOfSelf() {
        return memberDependenciesOnClass.getMethodThrowsDeclarationsWithTypeOfClass();
    }

    /**
     * @return Constructors of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getConstructorsWithParameterTypeOfSelf() {
        return memberDependenciesOnClass.getConstructorsWithParameterTypeOfClass();
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOfSelf() {
        return memberDependenciesOnClass.getConstructorsWithThrowsDeclarationTypeOfClass();
    }

    /**
     * @return All imported {@link JavaAnnotation JavaAnnotations} that have the annotation type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<?>> getAnnotationsWithTypeOfSelf() {
        return memberDependenciesOnClass.getAnnotationsWithTypeOfClass();
    }

    /**
     * @param clazz An arbitrary type
     * @return true, if this {@link JavaClass} represents the same class as the supplied {@link Class}, otherwise false
     */
    @PublicAPI(usage = ACCESS)
    public boolean isEquivalentTo(Class<?> clazz) {
        return getName().equals(clazz.getName());
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableFrom(Class<?> type) {
        return isAssignableFrom(type.getName());
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableFrom(String typeName) {
        return isAssignableFrom(GET_NAME.is(equalTo(typeName)));
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        List<JavaClass> possibleTargets = ImmutableList.<JavaClass>builder()
                .add(this).addAll(getAllSubClasses()).build();

        return anyMatches(possibleTargets, predicate);
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableTo(Class<?> type) {
        return isAssignableTo(type.getName());
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableTo(final String typeName) {
        return isAssignableTo(GET_NAME.is(equalTo(typeName)));
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        List<JavaClass> possibleTargets = ImmutableList.<JavaClass>builder()
                .addAll(getClassHierarchy()).addAll(getAllInterfaces()).build();

        return anyMatches(possibleTargets, predicate);
    }

    private boolean anyMatches(List<JavaClass> possibleTargets, DescribedPredicate<? super JavaClass> predicate) {
        for (JavaClass javaClass : possibleTargets) {
            if (predicate.apply(javaClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the respective {@link Class} from the classpath.<br>
     * NOTE: This method will throw an exception, if the respective {@link Class} or any of its dependencies
     * can't be found on the classpath.
     *
     * @return The {@link Class} equivalent to this {@link JavaClass}
     */
    @ResolvesTypesViaReflection
    @PublicAPI(usage = ACCESS)
    public Class<?> reflect() {
        return reflectSupplier.get();
    }

    void completeClassHierarchyFrom(ImportContext context) {
        completeSuperClassFrom(context);
        completeInterfacesFrom(context);
        allFields = Suppliers.memoize(new Supplier<Set<JavaField>>() {
            @Override
            public Set<JavaField> get() {
                ImmutableSet.Builder<JavaField> result = ImmutableSet.builder();
                for (JavaClass javaClass : concat(getClassHierarchy(), getAllInterfaces())) {
                    result.addAll(javaClass.getFields());
                }
                return result.build();
            }
        });
        allMethods = Suppliers.memoize(new Supplier<Set<JavaMethod>>() {
            @Override
            public Set<JavaMethod> get() {
                ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
                for (JavaClass javaClass : concat(getClassHierarchy(), getAllInterfaces())) {
                    result.addAll(javaClass.getMethods());
                }
                return result.build();
            }
        });
        allConstructors = Suppliers.memoize(new Supplier<Set<JavaConstructor>>() {
            @Override
            public Set<JavaConstructor> get() {
                ImmutableSet.Builder<JavaConstructor> result = ImmutableSet.builder();
                for (JavaClass javaClass : getClassHierarchy()) {
                    result.addAll(javaClass.getConstructors());
                }
                return result.build();
            }
        });
    }

    private void completeSuperClassFrom(ImportContext context) {
        superClass = context.createSuperClass(this);
        if (superClass.isPresent()) {
            superClass.get().subClasses.add(this);
        }
    }

    private void completeInterfacesFrom(ImportContext context) {
        interfaces.addAll(context.createInterfaces(this));
        for (JavaClass i : interfaces) {
            i.subClasses.add(this);
        }
    }

    void completeMembers(final ImportContext context) {
        fields = context.createFields(this);
        methods = context.createMethods(this);
        constructors = context.createConstructors(this);
        staticInitializer = context.createStaticInitializer(this);
        codeUnits = ImmutableSet.<JavaCodeUnit>builder()
                .addAll(methods).addAll(constructors).addAll(staticInitializer.asSet())
                .build();
        members = ImmutableSet.<JavaMember>builder()
                .addAll(fields)
                .addAll(methods)
                .addAll(constructors)
                .build();
        this.annotations = Suppliers.memoize(new Supplier<Map<String, JavaAnnotation<JavaClass>>>() {
            @Override
            public Map<String, JavaAnnotation<JavaClass>> get() {
                return context.createAnnotations(JavaClass.this);
            }
        });
    }

    CompletionProcess completeFrom(ImportContext context) {
        completeComponentType(context);
        enclosingClass = context.createEnclosingClass(this);
        memberDependenciesOnClass = new MemberDependenciesOnClass(
                context.getFieldsOfType(this),
                context.getMethodsWithParameterOfType(this),
                context.getMethodsWithReturnType(this),
                context.getMethodThrowsDeclarationsOfType(this),
                context.getConstructorsWithParameterOfType(this),
                context.getConstructorThrowsDeclarationsOfType(this),
                context.getAnnotationsOfType(this),
                context.getAnnotationsWithParameterOfType(this),
                context.getMembersAnnotatedWithType(this),
                context.getMembersWithParametersOfType(this));
        return new CompletionProcess();
    }

    private void completeComponentType(ImportContext context) {
        JavaClass current = this;
        while (current.isArray() && !current.componentType.isPresent()) {
            JavaClass componentType = context.resolveClass(current.javaType.tryGetComponentType().get().getName());
            current.componentType = Optional.of(componentType);
            current = componentType;
        }
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + javaType.getName() + "\'}";
    }

    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(Class<?>... paramTypes) {
        return namesOf(ImmutableList.copyOf(paramTypes));
    }

    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(List<Class<?>> paramTypes) {
        ArrayList<String> result = new ArrayList<>();
        for (Class<?> paramType : paramTypes) {
            result.add(paramType.getName());
        }
        return result;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isAnonymous() {
        return getSimpleName().isEmpty();
    }

    private Set<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAccess<?> access : filterNoSelfAccess(accesses)) {
            result.add(Dependency.from(access));
        }
        return result.build();
    }

    private Set<Dependency> inheritanceDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass superType : FluentIterable.from(getInterfaces()).append(getSuperClass().asSet())) {
            result.add(Dependency.fromInheritance(this, superType));
        }
        return result.build();
    }

    private Set<Dependency> fieldDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaField field : nonPrimitive(getFields(), GET_RAW_TYPE)) {
            result.add(Dependency.fromField(field));
        }
        return result.build();
    }

    private Set<Dependency> returnTypeDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : nonPrimitive(getMethods(), GET_RAW_RETURN_TYPE)) {
            result.add(Dependency.fromReturnType(method));
        }
        return result.build();
    }

    private Set<Dependency> methodParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaMethod method : getMethods()) {
            for (JavaClass parameter : nonPrimitive(method.getRawParameterTypes())) {
                result.add(Dependency.fromParameter(method, parameter));
            }
        }
        return result.build();
    }

    private Set<Dependency> throwsDeclarationDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : getCodeUnits()) {
            for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : codeUnit.getThrowsClause()) {
                result.add(Dependency.fromThrowsDeclaration(throwsDeclaration));
            }
        }
        return result.build();
    }

    private Set<Dependency> constructorParameterDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaConstructor constructor : getConstructors()) {
            for (JavaClass parameter : nonPrimitive(constructor.getRawParameterTypes())) {
                result.add(Dependency.fromParameter(constructor, parameter));
            }
        }
        return result.build();
    }

    private Set<Dependency> annotationDependenciesFromSelf() {
        return new ImmutableSet.Builder<Dependency>()
                .addAll(annotationDependencies(this))
                .addAll(annotationDependencies(getFields()))
                .addAll(annotationDependencies(getMethods()))
                .addAll(annotationDependencies(getConstructors()))
                .build();
    }

    private <T extends HasDescription & HasAnnotations<?>> Set<Dependency> annotationDependencies(Set<T> annotatedObjects) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (T annotated : annotatedObjects) {
            result.addAll(annotationDependencies(annotated));
        }
        return result.build();
    }

    private <T extends HasDescription & HasAnnotations<?>> Set<Dependency> annotationDependencies(T annotated) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAnnotation<?> annotation : annotated.getAnnotations()) {
            result.add(Dependency.fromAnnotation(annotation));
            result.addAll(annotationParametersDependencies(annotation));
        }
        return result.build();
    }

    private Set<Dependency> annotationParametersDependencies(JavaAnnotation<?> annotation) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (Map.Entry<String, Object> entry : annotation.getProperties().entrySet()) {
            Object value = entry.getValue();
            if (value.getClass().isArray()) {
                if (!value.getClass().getComponentType().isPrimitive()) {
                    Object[] values = (Object[]) value;
                    for (Object o : values) {
                        result.addAll(annotationParameterDependencies(annotation, o));
                    }
                }
            } else {
                result.addAll(annotationParameterDependencies(annotation, value));
            }
        }
        return result.build();
    }

    private Set<Dependency> annotationParameterDependencies(JavaAnnotation<?> origin, Object value) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        if (value instanceof JavaClass) {
            JavaClass annotationMember = (JavaClass) value;
            result.add(Dependency.fromAnnotationMember(origin, annotationMember));
        } else if (value instanceof JavaAnnotation<?>) {
            JavaAnnotation<?> nestedAnnotation = (JavaAnnotation<?>) value;
            result.add(Dependency.fromAnnotationMember(origin, nestedAnnotation.getRawType()));
            result.addAll(annotationParametersDependencies(nestedAnnotation));
        }
        return result.build();
    }

    private Set<Dependency> inheritanceDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaClass subClass : getSubClasses()) {
            result.add(Dependency.fromInheritance(subClass, this));
        }
        return result;
    }

    private Set<Dependency> fieldDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaField field : getFieldsWithTypeOfSelf()) {
            result.add(Dependency.fromField(field));
        }
        return result;
    }

    private Set<Dependency> returnTypeDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : getMethodsWithReturnTypeOfSelf()) {
            result.add(Dependency.fromReturnType(method));
        }
        return result;
    }

    private Set<Dependency> methodParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaMethod method : getMethodsWithParameterTypeOfSelf()) {
            result.add(Dependency.fromParameter(method, this));
        }
        return result;
    }

    private Set<Dependency> throwsDeclarationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (ThrowsDeclaration<? extends JavaCodeUnit> throwsDeclaration : memberDependenciesOnClass.getThrowsDeclarationsWithTypeOfClass()) {
            result.add(Dependency.fromThrowsDeclaration(throwsDeclaration));
        }
        return result;
    }

    private Set<Dependency> constructorParameterDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaConstructor constructor : getConstructorsWithParameterTypeOfSelf()) {
            result.add(Dependency.fromParameter(constructor, this));
        }
        return result;
    }

    private Iterable<? extends Dependency> annotationDependenciesToSelf() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAnnotation<?> annotation : getAnnotationsWithTypeOfSelf()) {
            result.add(Dependency.fromAnnotation(annotation));
        }
        for (JavaAnnotation<?> annotation : memberDependenciesOnClass.getAnnotationsWithParameterTypeOfClass()) {
            result.add(Dependency.fromAnnotationMember(annotation, this));
        }
        for (JavaMember member : memberDependenciesOnClass.getMembersWithAnnotationTypeOfClass()) {
            JavaAnnotation<?> annotation = member.getAnnotationOfType(getName());
            result.add(Dependency.fromAnnotation(annotation));
        }
        for (JavaMember member : memberDependenciesOnClass.getMembersWithAnnotationParameterTypeOfClass()) {
            JavaAnnotation<?> annotation = member.getAnnotationOfType(getName());
            result.add(Dependency.fromAnnotationMember(annotation, this));
        }
        return result;
    }

    private Set<JavaAccess<?>> filterNoSelfAccess(Set<? extends JavaAccess<?>> accesses) {
        Set<JavaAccess<?>> result = new HashSet<>();
        for (JavaAccess<?> access : accesses) {
            if (!access.getTargetOwner().equals(access.getOriginOwner())) {
                result.add(access);
            }
        }
        return result;
    }

    private Set<JavaClass> nonPrimitive(Collection<JavaClass> classes) {
        return nonPrimitive(classes, com.tngtech.archunit.base.Function.Functions.<JavaClass>identity());
    }

    private <T> Set<T> nonPrimitive(Collection<T> members, Function<? super T, JavaClass> getRelevantType) {
        ImmutableSet.Builder<T> result = ImmutableSet.builder();
        for (T member : members) {
            if (!getRelevantType.apply(member).isPrimitive()) {
                result.add(member);
            }
        }
        return result.build();
    }

    private static class MemberDependenciesOnClass {
        private final Set<JavaField> fieldsWithTypeOfClass;
        private final Set<JavaMethod> methodsWithParameterTypeOfClass;
        private final Set<JavaMethod> methodsWithReturnTypeOfClass;
        private final Set<ThrowsDeclaration<JavaMethod>> methodsWithThrowsDeclarationTypeOfClass;
        private final Set<JavaConstructor> constructorsWithParameterTypeOfClass;
        private final Set<ThrowsDeclaration<JavaConstructor>> constructorsWithThrowsDeclarationTypeOfClass;
        private final Set<JavaAnnotation<?>> annotationsWithTypeOfClass;
        private final Set<JavaAnnotation<?>> annotationsWithParameterTypeOfClass;
        private final Set<JavaMember> membersWithAnnotationTypeOfClass;
        private final Set<JavaMember> membersWithAnnotationParameterTypeOfClass;

        MemberDependenciesOnClass(
                Set<JavaField> fieldsWithTypeOfClass,
                Set<JavaMethod> methodsWithParameterTypeOfClass,
                Set<JavaMethod> methodsWithReturnTypeOfClass,
                Set<ThrowsDeclaration<JavaMethod>> methodsWithThrowsDeclarationTypeOfClass,
                Set<JavaConstructor> constructorsWithParameterTypeOfClass,
                Set<ThrowsDeclaration<JavaConstructor>> constructorsWithThrowsDeclarationTypeOfClass,
                Set<JavaAnnotation<?>> annotationsWithTypeOfClass,
                Set<JavaAnnotation<?>> annotationsWithParameterTypeOfClass,
                Set<JavaMember> membersWithAnnotationTypeOfClass,
                Set<JavaMember> membersWithAnnotationParameterTypeOfClass) {

            this.fieldsWithTypeOfClass = ImmutableSet.copyOf(fieldsWithTypeOfClass);
            this.methodsWithParameterTypeOfClass = ImmutableSet.copyOf(methodsWithParameterTypeOfClass);
            this.methodsWithReturnTypeOfClass = ImmutableSet.copyOf(methodsWithReturnTypeOfClass);
            this.methodsWithThrowsDeclarationTypeOfClass = ImmutableSet.copyOf(methodsWithThrowsDeclarationTypeOfClass);
            this.constructorsWithParameterTypeOfClass = ImmutableSet.copyOf(constructorsWithParameterTypeOfClass);
            this.constructorsWithThrowsDeclarationTypeOfClass = ImmutableSet.copyOf(constructorsWithThrowsDeclarationTypeOfClass);
            this.annotationsWithTypeOfClass = ImmutableSet.copyOf(annotationsWithTypeOfClass);
            this.annotationsWithParameterTypeOfClass = ImmutableSet.copyOf(annotationsWithParameterTypeOfClass);
            this.membersWithAnnotationTypeOfClass = ImmutableSet.copyOf(membersWithAnnotationTypeOfClass);
            this.membersWithAnnotationParameterTypeOfClass = ImmutableSet.copyOf(membersWithAnnotationParameterTypeOfClass);
        }

        Set<JavaField> getFieldsWithTypeOfClass() {
            return fieldsWithTypeOfClass;
        }

        Set<JavaMethod> getMethodsWithParameterTypeOfClass() {
            return methodsWithParameterTypeOfClass;
        }

        Set<JavaMethod> getMethodsWithReturnTypeOfClass() {
            return methodsWithReturnTypeOfClass;
        }

        Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOfClass() {
            return methodsWithThrowsDeclarationTypeOfClass;
        }

        Set<JavaConstructor> getConstructorsWithParameterTypeOfClass() {
            return constructorsWithParameterTypeOfClass;
        }

        Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOfClass() {
            return constructorsWithThrowsDeclarationTypeOfClass;
        }

        Set<ThrowsDeclaration<? extends JavaCodeUnit>> getThrowsDeclarationsWithTypeOfClass() {
            return union(methodsWithThrowsDeclarationTypeOfClass, constructorsWithThrowsDeclarationTypeOfClass);
        }

        Set<JavaAnnotation<?>> getAnnotationsWithTypeOfClass() {
            return annotationsWithTypeOfClass;
        }

        Set<JavaAnnotation<?>> getAnnotationsWithParameterTypeOfClass() {
            return annotationsWithParameterTypeOfClass;
        }

        Set<JavaMember> getMembersWithAnnotationTypeOfClass() {
            return membersWithAnnotationTypeOfClass;
        }

        Set<JavaMember> getMembersWithAnnotationParameterTypeOfClass() {
            return membersWithAnnotationParameterTypeOfClass;
        }
    }

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, String> GET_SIMPLE_NAME = new ChainableFunction<JavaClass, String>() {
            @Override
            public String apply(JavaClass input) {
                return input.getSimpleName();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, String> GET_PACKAGE_NAME = new ChainableFunction<JavaClass, String>() {
            @Override
            public String apply(JavaClass input) {
                return input.getPackageName();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, JavaPackage> GET_PACKAGE = new ChainableFunction<JavaClass, JavaPackage>() {
            @Override
            public JavaPackage apply(JavaClass input) {
                return input.getPackage();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMember>> GET_MEMBERS = new ChainableFunction<JavaClass, Set<JavaMember>>() {
            @Override
            public Set<JavaMember> apply(JavaClass input) {
                return input.getMembers();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaField>> GET_FIELDS = new ChainableFunction<JavaClass, Set<JavaField>>() {
            @Override
            public Set<JavaField> apply(JavaClass input) {
                return input.getFields();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCodeUnit>> GET_CODE_UNITS =
                new ChainableFunction<JavaClass, Set<JavaCodeUnit>>() {
                    @Override
                    public Set<JavaCodeUnit> apply(JavaClass input) {
                        return input.getCodeUnits();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethod>> GET_METHODS = new ChainableFunction<JavaClass, Set<JavaMethod>>() {
            @Override
            public Set<JavaMethod> apply(JavaClass input) {
                return input.getMethods();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructor>> GET_CONSTRUCTORS =
                new ChainableFunction<JavaClass, Set<JavaConstructor>>() {
                    @Override
                    public Set<JavaConstructor> apply(JavaClass input) {
                        return input.getConstructors();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaFieldAccess>> GET_FIELD_ACCESSES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaFieldAccess>>() {
                    @Override
                    public Set<JavaFieldAccess> apply(JavaClass input) {
                        return input.getFieldAccessesFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethodCall>> GET_METHOD_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaMethodCall>>() {
                    @Override
                    public Set<JavaMethodCall> apply(JavaClass input) {
                        return input.getMethodCallsFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructorCall>> GET_CONSTRUCTOR_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaConstructorCall>>() {
                    @Override
                    public Set<JavaConstructorCall> apply(JavaClass input) {
                        return input.getConstructorCallsFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCall<?>>> GET_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaCall<?>>>() {
                    @Override
                    public Set<JavaCall<?>> apply(JavaClass input) {
                        return input.getCallsFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaAccess<?>>> GET_ACCESSES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaAccess<?>>>() {
                    @Override
                    public Set<JavaAccess<?>> apply(JavaClass input) {
                        return input.getAccessesFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<Dependency>> GET_DIRECT_DEPENDENCIES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<Dependency>>() {
                    @Override
                    public Set<Dependency> apply(JavaClass input) {
                        return input.getDirectDependenciesFromSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaAccess<?>>> GET_ACCESSES_TO_SELF =
                new ChainableFunction<JavaClass, Set<JavaAccess<?>>>() {
                    @Override
                    public Set<JavaAccess<?>> apply(JavaClass input) {
                        return input.getAccessesToSelf();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<Dependency>> GET_DIRECT_DEPENDENCIES_TO_SELF =
                new ChainableFunction<JavaClass, Set<Dependency>>() {
                    @Override
                    public Set<Dependency> apply(JavaClass input) {
                        return input.getDirectDependenciesToSelf();
                    }
                };
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> INTERFACES = new DescribedPredicate<JavaClass>("interfaces") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isInterface();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> ENUMS = new DescribedPredicate<JavaClass>("enums") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isEnum();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> type(final Class<?> type) {
            return equalTo(type.getName()).<JavaClass>onResultOf(GET_NAME).as("type " + type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> simpleName(final String name) {
            return equalTo(name).onResultOf(GET_SIMPLE_NAME).as("simple name '%s'", name);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> simpleNameStartingWith(final String prefix) {
            return new SimpleNameStartingWithPredicate(prefix);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> simpleNameContaining(final String infix) {
            return new SimpleNameContainingPredicate(infix);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> simpleNameEndingWith(final String suffix) {
            return new SimpleNameEndingWithPredicate(suffix);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final Class<?> type) {
            return assignableTo(type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final Class<?> type) {
            return assignableFrom(type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final String typeName) {
            return assignableTo(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final String typeName) {
            return assignableFrom(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final DescribedPredicate<? super JavaClass> predicate) {
            return new AssignableToPredicate(predicate);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final DescribedPredicate<? super JavaClass> predicate) {
            return new AssignableFromPredicate(predicate);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final Class<?> type) {
            if (!type.isInterface()) {
                throw new InvalidSyntaxUsageException(String.format(
                        "implement(type) can only ever be true, if type is an interface, but type %s is not", type.getName()));
            }
            return implement(type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final String typeName) {
            return implement(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final DescribedPredicate<? super JavaClass> predicate) {
            DescribedPredicate<JavaClass> selfIsImplementation = not(INTERFACES);
            DescribedPredicate<JavaClass> interfacePredicate = predicate.<JavaClass>forSubType().and(INTERFACES);
            return selfIsImplementation.and(assignableTo(interfacePredicate))
                    .as("implement " + predicate.getDescription());
        }

        /**
         * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
         * characters, '..' stands for any sequence of packages.
         * For further details see {@link PackageMatcher}.
         *
         * @param packageIdentifier A string representing the identifier to match packages against
         * @return A {@link DescribedPredicate} returning true iff the package of the
         * tested {@link JavaClass} matches the identifier
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> resideInAPackage(final String packageIdentifier) {
            return resideInAnyPackage(new String[]{packageIdentifier},
                    String.format("reside in a package '%s'", packageIdentifier));
        }

        /**
         * @see #resideInAPackage(String)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> resideInAnyPackage(final String... packageIdentifiers) {
            return resideInAnyPackage(packageIdentifiers,
                    String.format("reside in any package ['%s']", Joiner.on("', '").join(packageIdentifiers)));
        }

        private static DescribedPredicate<JavaClass> resideInAnyPackage(final String[] packageIdentifiers, final String description) {
            final Set<PackageMatcher> packageMatchers = new HashSet<>();
            for (String identifier : packageIdentifiers) {
                packageMatchers.add(PackageMatcher.of(identifier));
            }
            return new PackageMatchesPredicate(packageMatchers, description);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> resideOutsideOfPackage(String packageIdentifier) {
            return not(resideInAPackage(packageIdentifier))
                    .as("reside outside of package '%s'", packageIdentifier);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> resideOutsideOfPackages(String... packageIdentifiers) {
            return not(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers))
                    .as("reside outside of packages ['%s']", Joiner.on("', '").join(packageIdentifiers));
        }

        /**
         * @see JavaClass#isEquivalentTo(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> equivalentTo(final Class<?> clazz) {
            return new EquivalentToPredicate(clazz);
        }

        /**
         * A predicate to determine if a {@link JavaClass} "belongs" to one of the passed {@link Class classes},
         * where we define "belong" as being equivalent to the class itself or any inner/anonymous class of this class.
         *
         * @param classes The {@link Class classes} to check the {@link JavaClass} against
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} is equivalent to
         * one of the supplied {@link Class classes} or to one of its inner/anonymous classes.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> belongToAnyOf(Class<?>... classes) {
            return new BelongToAnyOfPredicate(classes);
        }

        private static class BelongToAnyOfPredicate extends DescribedPredicate<JavaClass> {
            private final Class<?>[] classes;

            BelongToAnyOfPredicate(Class<?>... classes) {
                super("belong to any of " + JavaClass.namesOf(classes));
                this.classes = classes;
            }

            @Override
            public boolean apply(JavaClass input) {
                for (Class<?> clazz : classes) {
                    if (belongsTo(input, clazz)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean belongsTo(JavaClass input, Class<?> clazz) {
                JavaClass toTest = input;
                while (!toTest.isEquivalentTo(clazz) && toTest.getEnclosingClass().isPresent()) {
                    toTest = toTest.getEnclosingClass().get();
                }
                return toTest.isEquivalentTo(clazz);
            }
        }

        private static class SimpleNameStartingWithPredicate extends DescribedPredicate<JavaClass> {
            private final String prefix;

            SimpleNameStartingWithPredicate(String prefix) {
                super(String.format("simple name starting with '%s'", prefix));
                this.prefix = prefix;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.getSimpleName().startsWith(prefix);
            }
        }

        private static class SimpleNameContainingPredicate extends DescribedPredicate<JavaClass> {
            private final String infix;

            SimpleNameContainingPredicate(String infix) {
                super(String.format("simple name containing '%s'", infix));
                this.infix = infix;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.getSimpleName().contains(infix);
            }
        }

        private static class SimpleNameEndingWithPredicate extends DescribedPredicate<JavaClass> {
            private final String suffix;

            SimpleNameEndingWithPredicate(String suffix) {
                super(String.format("simple name ending with '%s'", suffix));
                this.suffix = suffix;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.getSimpleName().endsWith(suffix);
            }
        }

        private static class AssignableToPredicate extends DescribedPredicate<JavaClass> {
            private final DescribedPredicate<? super JavaClass> predicate;

            AssignableToPredicate(DescribedPredicate<? super JavaClass> predicate) {
                super("assignable to " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.isAssignableTo(predicate);
            }
        }

        private static class AssignableFromPredicate extends DescribedPredicate<JavaClass> {
            private final DescribedPredicate<? super JavaClass> predicate;

            AssignableFromPredicate(DescribedPredicate<? super JavaClass> predicate) {
                super("assignable from " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.isAssignableFrom(predicate);
            }
        }

        private static class PackageMatchesPredicate extends DescribedPredicate<JavaClass> {
            private final Set<PackageMatcher> packageMatchers;

            PackageMatchesPredicate(Set<PackageMatcher> packageMatchers, String description) {
                super(description);
                this.packageMatchers = packageMatchers;
            }

            @Override
            public boolean apply(JavaClass input) {
                for (PackageMatcher matcher : packageMatchers) {
                    if (matcher.matches(input.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
        }

        private static class EquivalentToPredicate extends DescribedPredicate<JavaClass> {
            private final Class<?> clazz;

            EquivalentToPredicate(Class<?> clazz) {
                super("equivalent to %s", clazz.getName());
                this.clazz = clazz;
            }

            @Override
            public boolean apply(JavaClass input) {
                return input.isEquivalentTo(clazz);
            }
        }
    }

    class CompletionProcess {
        AccessContext.Part completeCodeUnitsFrom(ImportContext context) {
            AccessContext.Part part = new AccessContext.Part();
            for (JavaCodeUnit codeUnit : codeUnits) {
                part.mergeWith(codeUnit.completeFrom(context));
            }
            return part;
        }
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    private class ReflectClassSupplier implements Supplier<Class<?>> {
        @Override
        public Class<?> get() {
            return javaType.resolveClass(getCurrentClassLoader(getClass()));
        }
    }
}
