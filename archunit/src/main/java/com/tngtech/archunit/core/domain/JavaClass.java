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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
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
import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext.AccessContext;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

public class JavaClass implements HasName, HasAnnotations, HasModifiers {
    private final Optional<Source> source;
    private final JavaType javaType;
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
    private Supplier<Map<String, JavaAnnotation>> annotations =
            Suppliers.ofInstance(Collections.<String, JavaAnnotation>emptyMap());
    private Supplier<Set<JavaMethod>> allMethods;
    private Supplier<Set<JavaConstructor>> allConstructors;
    private Supplier<Set<JavaField>> allFields;
    private Supplier<Set<JavaMember>> allMembers = Suppliers.memoize(new Supplier<Set<JavaMember>>() {
        @Override
        public Set<JavaMember> get() {
            return ImmutableSet.<JavaMember>builder()
                    .addAll(getAllFields())
                    .addAll(getAllMethods())
                    .addAll(getAllConstructors())
                    .build();
        }
    });

    JavaClass(JavaClassBuilder builder) {
        source = checkNotNull(builder.getSource());
        javaType = checkNotNull(builder.getJavaType());
        isInterface = builder.isInterface();
        isEnum = builder.isEnum();
        modifiers = checkNotNull(builder.getModifiers());
        reflectSupplier = Suppliers.memoize(new ReflectClassSupplier());
    }

    @PublicAPI(usage = ACCESS)
    public Optional<Source> getSource() {
        return source;
    }

    @Override
    public String getName() {
        return javaType.getName();
    }

    @PublicAPI(usage = ACCESS)
    public String getSimpleName() {
        return javaType.getSimpleName();
    }

    @PublicAPI(usage = ACCESS)
    public String getPackage() {
        return javaType.getPackage();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isInterface() {
        return isInterface;
    }

    @PublicAPI(usage = ACCESS)
    public boolean isEnum() {
        return isEnum;
    }

    @Override
    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }

    @Override
    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.get().containsKey(annotationTypeName);
    }

    @Override
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.get().values(), predicate);
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return An {@link Annotation} of the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #tryGetAnnotationOfType(Class)
     */
    @Override
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    @Override
    public JavaAnnotation getAnnotationOfType(String typeName) {
        return tryGetAnnotationOfType(typeName).getOrThrow(new IllegalArgumentException(
                String.format("Type %s is not annotated with @%s", getSimpleName(), Formatters.ensureSimpleName(typeName))));
    }

    @Override
    public Set<JavaAnnotation> getAnnotations() {
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
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        return tryGetAnnotationOfType(type.getName()).transform(toAnnotationOfType(type));
    }

    /**
     * Same as {@link #tryGetAnnotationOfType(Class)}, but takes the type name.
     */
    @Override
    public Optional<JavaAnnotation> tryGetAnnotationOfType(String typeName) {
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
        return tryFindMatchingCodeUnit(codeUnits, name, parameters).getOrThrow(new IllegalArgumentException("No code unit with name '" + name + "' and parameters " + parameters +
                " in codeUnits " + codeUnits + " of class " + getName()));
    }

    private <T extends JavaCodeUnit> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<String> parameters) {
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(codeUnit.getParameters().getNames())) {
                return Optional.of(codeUnit);
            }
        }
        return Optional.absent();
    }

    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(methods, name, namesOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name, Class<?>... parameters) {
        return tryFindMatchingCodeUnit(methods, name, namesOf(parameters));
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
    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, namesOf(parameters));
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
        return Sets.union(getFieldAccessesFromSelf(), getCallsFromSelf());
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
        return Sets.union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
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
     * @deprecated Use {@link #getDirectDependenciesFromSelf()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependencies() {
        return getDirectDependenciesFromSelf();
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
     * </ul>
     *
     * @return All dependencies originating directly from this class (i.e. where this class is the origin)
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = dependenciesFromAccesses(getAccessesFromSelf());
        for (JavaClass superType : FluentIterable.from(getInterfaces()).append(getSuperClass().asSet())) {
            result.add(Dependency.fromInheritance(this, superType));
        }
        return result.build();
    }

    private ImmutableSet.Builder<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaAccess<?> access : filterNoSelfAccess(accesses)) {
            result.add(Dependency.from(access));
        }
        return result;
    }

    /**
     * Like {@link #getDirectDependenciesFromSelf()}, but instead returns all dependencies where this class
     * is target.
     *
     * @return Dependencies where this class is the target.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesToSelf() {
        ImmutableSet.Builder<Dependency> result = dependenciesFromAccesses(getAccessesToSelf());
        for (JavaClass subClass : getSubClasses()) {
            result.add(Dependency.fromInheritance(subClass, this));
        }
        return result.build();
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
        this.annotations = Suppliers.memoize(new Supplier<Map<String, JavaAnnotation>>() {
            @Override
            public Map<String, JavaAnnotation> get() {
                return context.createAnnotations(JavaClass.this);
            }
        });
    }

    CompletionProcess completeFrom(ImportContext context) {
        enclosingClass = context.createEnclosingClass(this);
        return new CompletionProcess();
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
        return getSimpleName().isEmpty(); // This is implemented the same way within java.lang.Class
    }

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final Function<JavaClass, String> SIMPLE_NAME = new Function<JavaClass, String>() {
            @Override
            public String apply(JavaClass input) {
                return input.getSimpleName();
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
        public static DescribedPredicate<JavaClass> type(final Class<?> type) {
            return equalTo(type.getName()).<JavaClass>onResultOf(GET_NAME).as("type " + type.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> simpleName(final String name) {
            return equalTo(name).onResultOf(SIMPLE_NAME).as("simple name '%s'", name);
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
            return new DescribedPredicate<JavaClass>("assignable to " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isAssignableTo(predicate);
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final DescribedPredicate<? super JavaClass> predicate) {
            return new DescribedPredicate<JavaClass>("assignable from " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isAssignableFrom(predicate);
                }
            };
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
            return new DescribedPredicate<JavaClass>(description) {
                @Override
                public boolean apply(JavaClass input) {
                    for (PackageMatcher matcher : packageMatchers) {
                        if (matcher.matches(input.getPackage())) {
                            return true;
                        }
                    }
                    return false;
                }
            };
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
            return new DescribedPredicate<JavaClass>("equivalent to %s", clazz.getName()) {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isEquivalentTo(clazz);
                }
            };
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
            return javaType.resolveClass(getClass().getClassLoader());
        }
    }
}
