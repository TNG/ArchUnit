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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.ENUM;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

public class JavaClass implements HasName.AndFullName, HasAnnotations<JavaClass>, HasModifiers, HasSourceCodeLocation {
    private final Optional<Source> source;
    private final SourceCodeLocation sourceCodeLocation;
    private final JavaType javaType;
    private JavaPackage javaPackage;
    private final boolean isInterface;
    private final boolean isEnum;
    private final boolean isAnonymousClass;
    private final boolean isMemberClass;
    private final Set<JavaModifier> modifiers;
    private final Supplier<Class<?>> reflectSupplier;
    private Set<JavaField> fields = emptySet();
    private Set<JavaCodeUnit> codeUnits = emptySet();
    private Set<JavaMethod> methods = emptySet();
    private Set<JavaMember> members = emptySet();
    private Set<JavaConstructor> constructors = emptySet();
    private Optional<JavaStaticInitializer> staticInitializer = Optional.absent();
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> interfaces = new HashSet<>();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();
    private Optional<JavaClass> componentType = Optional.absent();
    private Map<String, JavaAnnotation<JavaClass>> annotations = emptyMap();
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
    private JavaClassDependencies javaClassDependencies;

    JavaClass(JavaClassBuilder builder) {
        source = checkNotNull(builder.getSource());
        javaType = checkNotNull(builder.getJavaType());
        isInterface = builder.isInterface();
        isEnum = builder.isEnum();
        isAnonymousClass = builder.isAnonymousClass();
        isMemberClass = builder.isMemberClass();
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
    public Optional<JavaEnumConstant> tryGetEnumConstant(String name) {
        Optional<JavaField> field = tryGetField(name);
        if (!field.isPresent() || !field.get().getModifiers().contains(ENUM)) {
            return Optional.absent();
        }
        return Optional.of(new JavaEnumConstant(this, field.get().getName()));
    }

    @PublicAPI(usage = ACCESS)
    public JavaEnumConstant getEnumConstant(String name) {
        Optional<JavaEnumConstant> enumConstant = tryGetEnumConstant(name);
        checkArgument(enumConstant.isPresent(), "There exists no enum constant with name '%s' in class %s", name, getName());
        return enumConstant.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaEnumConstant> getEnumConstants() {
        ImmutableSet.Builder<JavaEnumConstant> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            if (field.getModifiers().contains(ENUM)) {
                result.add(new JavaEnumConstant(this, field.getName()));
            }
        }
        return result.build();
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
     * A <b>top level class</b> is a class that is not a nested class, i.e. not declared within the body
     * of another class.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class NestedNonStatic {}
     *     static class NestedStatic {}
     *
     *     void method() {
     *         class NestedLocal {}
     *
     *         new NestedAnonymous() {}
     *     }
     * }
     * </code></pre>
     * Of all these class declarations only {@code TopLevel} is a top level class, since all
     * other classes are declared within the body of {@code TopLevel} and are thereby nested classes.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isNestedClass()
     * @see #isMemberClass()
     * @see #isInnerClass()
     * @see #isLocalClass()
     * @see #isAnonymousClass()
     * @return {@code true} if this class is a top level class, i.e. not nested inside of
     *         any other class, {@code false} otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean isTopLevelClass() {
        return !isNestedClass();
    }

    /**
     * A <b>nested class</b> is any class whose declaration occurs
     * within the body of another class or interface.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class NestedNonStatic {}
     *     static class NestedStatic {}
     *
     *     void method() {
     *         class NestedLocal {}
     *
     *         new NestedAnonymous() {}
     *     }
     * }
     * </code></pre>
     * All classes {@code NestedNonStatic}, {@code NestedStatic}, {@code NestedLocal} and the class
     * the compiler creates for the anonymous class derived from {@code "new NestedAnonymous() {}"}
     * (which will have some generated name like {@code TopLevel$1})
     * are considered nested classes. {@code TopLevel} on the other side is no nested class.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isTopLevelClass()
     * @see #isMemberClass()
     * @see #isInnerClass()
     * @see #isLocalClass()
     * @see #isAnonymousClass()
     * @return {@code true} if this class is nested, i.e. declared within another class,
     *         {@code false} otherwise (i.e. for top-level classes)
     */
    @PublicAPI(usage = ACCESS)
    public boolean isNestedClass() {
        return enclosingClass.isPresent();
    }

    /**
     * A <b>member class</b> is a class whose declaration is <u>directly</u> enclosed
     * in the body of another class or interface declaration.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class MemberClassNonStatic {}
     *     static class MemberClassStatic {}
     *
     *     void method() {
     *         class NoMemberLocal {}
     *
     *         new NoMemberAnonymous() {}
     *     }
     * }
     * </code></pre>
     * Both {@code MemberClassNonStatic} and {@code MemberClassStatic} are member classes,
     * since they are directly declared within the body of {@code TopLevel}.
     * On the other hand {@code NoMemberLocal} and the class
     * the compiler creates for the anonymous class derived from {@code "new NoMemberAnonymous() {}"}
     * (which will have some generated name like {@code TopLevel$1}), as well as {@code TopLevel}
     * itself, are not considered member classes.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isTopLevelClass()
     * @see #isNestedClass()
     * @see #isInnerClass()
     * @see #isLocalClass()
     * @see #isAnonymousClass()
     * @return {@code true} if this class is a member class, i.e. directly declared within
     *         the body of another class, {@code false} otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean isMemberClass() {
        return isNestedClass() && isMemberClass;
    }

    /**
     * An <b>inner class</b> is a nested class that is not explicitly or implicitly declared static.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class InnerMemberClass {}
     *     static class NoInnerClassSinceDeclaredStatic {}
     *     interface NoInnerClassSinceInterface {}
     *
     *     void method() {
     *         class InnerLocalClass {}
     *
     *         new InnerAnonymousClass() {}
     *     }
     * }
     * </code></pre>
     * The classes {@code InnerMemberClass}, {@code InnerLocalClass} and the class
     * the compiler creates for the anonymous class derived from {@code "new InnerAnonymousClass() {}"}
     * (which will have some generated name like {@code TopLevel$1})
     * are inner classes since they are nested but not static.
     * On the other hand {@code NoInnerClassSinceDeclaredStatic}, {@code NoInnerClassSinceInterface} and {@code TopLevel}
     * are no inner classes, because the former two explicitly or implicitly have the
     * {@code static} modifier while the latter one is a top level class.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isTopLevelClass()
     * @see #isNestedClass()
     * @see #isMemberClass()
     * @see #isLocalClass()
     * @see #isAnonymousClass()
     * @return {@code true} if this class is an inner class (i.e. nested but non-static)
     *         {@code false} otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean isInnerClass() {
        return isNestedClass() && !getModifiers().contains(JavaModifier.STATIC);
    }

    /**
     * A <b>local class</b> is a nested class that is not a member of any class and that has a name.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class InnerClass {}
     *     static class NestedStaticClass {}
     *
     *     void method() {
     *         class LocalClass {}
     *
     *         new AnonymousClass() {}
     *     }
     * }
     * </code></pre>
     * Only The class {@code LocalClass} is a local class, since it is a nested class that is not a member
     * class, but it has the name "LocalClass".<br>
     * All the other classes {@code TopLevel}, {@code InnerClass}, {@code NestedStaticClass} and
     * the class the compiler creates for the anonymous class derived from {@code "new AnonymousClass() {}"}
     * (which will have some generated name like {@code TopLevel$1}) are considered non-local, since they
     * either are top level, directly declared within the body of {@code TopLevel} or are anonymous
     * and thus have no name.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isTopLevelClass()
     * @see #isNestedClass()
     * @see #isMemberClass()
     * @see #isInnerClass()
     * @see #isAnonymousClass()
     * @return {@code true} if this class is local class,
     *         {@code false} otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean isLocalClass() {
        return isNestedClass() && !isMemberClass() && !getSimpleName().isEmpty();
    }

    /**
     * An <b>anonymous class</b> is an inner class that is automatically derived from a class
     * creation expression with a declared class body, e.g. {@code new Example(){ <some-body> }}.<br>
     * The compiler will automatically create a class backing this instance, typically with an
     * autogenerated name like {@code SomeClass$1}, where {@code SomeClass} is the class holding
     * the class creation expression.<br><br>
     *
     * Example:
     * <pre><code>
     * public class TopLevel {
     *     class InnerClass {}
     *     static class NestedStaticClass {}
     *
     *     void method() {
     *         class LocalClass {}
     *
     *         new AnonymousClass() {}
     *     }
     * }
     * </code></pre>
     * Only the class the compiler creates for the anonymous class derived from {@code "new AnonymousClass() {}"}
     * (which will have some generated name like {@code TopLevel$1}) is considered an anonymous class.<br>
     * All the other classes {@code TopLevel}, {@code InnerClass}, {@code NestedStaticClass} and
     * {@code LocalClass} are considered non-anonymous.
     * <br><br>
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se13/html/jls-8.html">
     *     Java Language Specification</a>
     *
     * @see #isTopLevelClass()
     * @see #isNestedClass()
     * @see #isMemberClass()
     * @see #isInnerClass()
     * @see #isLocalClass()
     * @return {@code true} if this class is an anonymous class,
     *         {@code false} otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean isAnonymousClass() {
        return isAnonymousClass;
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
        return annotations.containsKey(annotationTypeName);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return CanBeAnnotated.Utils.isAnnotatedWith(annotations.values(), predicate);
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
        return CanBeAnnotated.Utils.isMetaAnnotatedWith(annotations.values(), predicate);
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
        return ImmutableSet.copyOf(annotations.values());
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
        return Optional.fromNullable(annotations.get(typeName));
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
        return javaClassDependencies.getDirectDependenciesFromClass();
    }

    /**
     * Like {@link #getDirectDependenciesFromSelf()}, but instead returns all dependencies where this class
     * is target.
     *
     * @return Dependencies where this class is the target.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesToSelf() {
        return javaClassDependencies.getDirectDependenciesToClass();
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
        return javaClassDependencies.getFieldsWithTypeOfClass();
    }

    /**
     * @return Methods of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithParameterTypeOfSelf() {
        return javaClassDependencies.getMethodsWithParameterTypeOfClass();
    }

    /**
     * @return Methods of all imported classes that have a return type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithReturnTypeOfSelf() {
        return javaClassDependencies.getMethodsWithReturnTypeOfClass();
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOfSelf() {
        return javaClassDependencies.getMethodThrowsDeclarationsWithTypeOfClass();
    }

    /**
     * @return Constructors of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getConstructorsWithParameterTypeOfSelf() {
        return javaClassDependencies.getConstructorsWithParameterTypeOfClass();
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOfSelf() {
        return javaClassDependencies.getConstructorsWithThrowsDeclarationTypeOfClass();
    }

    /**
     * @return All imported {@link JavaAnnotation JavaAnnotations} that have the annotation type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<?>> getAnnotationsWithTypeOfSelf() {
        return javaClassDependencies.getAnnotationsWithTypeOfClass();
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
    }

    void completeAnnotations(final ImportContext context) {
        this.annotations = context.createAnnotations(this);
    }

    CompletionProcess completeFrom(ImportContext context) {
        completeComponentType(context);
        enclosingClass = context.createEnclosingClass(this);
        javaClassDependencies = new JavaClassDependencies(this, context);
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
        return "JavaClass{name='" + javaType.getName() + "'}";
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

    /**
     * @deprecated use {@link #isAnonymousClass()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public boolean isAnonymous() {
        return isAnonymousClass();
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
        public static final DescribedPredicate<JavaClass> TOP_LEVEL_CLASSES = new DescribedPredicate<JavaClass>("top level classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isTopLevelClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> NESTED_CLASSES = new DescribedPredicate<JavaClass>("nested classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isNestedClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> MEMBER_CLASSES = new DescribedPredicate<JavaClass>("member classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isMemberClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> INNER_CLASSES = new DescribedPredicate<JavaClass>("inner classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isInnerClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> LOCAL_CLASSES = new DescribedPredicate<JavaClass>("local classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isLocalClass();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> ANONYMOUS_CLASSES = new DescribedPredicate<JavaClass>("anonymous classes") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnonymousClass();
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
