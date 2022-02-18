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

import java.lang.annotation.Annotation;
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
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.base.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.domain.properties.HasTypeParameters;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CODE_UNITS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTORS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELDS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_MEMBERS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_METHODS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_STATIC_INITIALIZER;
import static com.tngtech.archunit.core.domain.JavaModifier.ENUM;
import static com.tngtech.archunit.core.domain.JavaType.Functions.TO_ERASURE;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Utils.toAnnotationOfType;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class JavaClass
        implements JavaType, HasName.AndFullName, HasTypeParameters<JavaClass>, HasAnnotations<JavaClass>, HasModifiers, HasSourceCodeLocation {

    private final Optional<Source> source;
    private final SourceCodeLocation sourceCodeLocation;
    private final JavaClassDescriptor descriptor;
    private JavaPackage javaPackage;
    private final boolean isInterface;
    private final boolean isEnum;
    private final boolean isAnnotation;
    private final boolean isRecord;
    private final boolean isAnonymousClass;
    private final boolean isMemberClass;
    private final Set<JavaModifier> modifiers;
    private List<JavaTypeVariable<JavaClass>> typeParameters = emptyList();
    private final Supplier<Class<?>> reflectSupplier;
    private JavaClassMembers members = JavaClassMembers.empty(this);
    private Superclass superclass = Superclass.ABSENT;
    private final Supplier<List<JavaClass>> allRawSuperclasses = Suppliers.memoize(new Supplier<List<JavaClass>>() {
        @Override
        public List<JavaClass> get() {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            JavaClass current = JavaClass.this;
            while (current.getRawSuperclass().isPresent()) {
                current = current.getRawSuperclass().get();
                result.add(current);
            }
            return result.build();
        }
    });
    private Interfaces interfaces = Interfaces.EMPTY;
    private final Supplier<Set<JavaClass>> allRawInterfaces = Suppliers.memoize(new Supplier<Set<JavaClass>>() {
        @Override
        public Set<JavaClass> get() {
            ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
            for (JavaClass i : interfaces.getRaw()) {
                result.add(i);
                result.addAll(i.getAllRawInterfaces());
            }
            result.addAll(superclass.getAllRawInterfaces());
            return result.build();
        }
    });
    private final Supplier<List<JavaClass>> classHierarchy = Suppliers.memoize(new Supplier<List<JavaClass>>() {
        @Override
        public List<JavaClass> get() {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            result.add(JavaClass.this);
            result.addAll(getAllRawSuperclasses());
            return result.build();
        }
    });
    private final Set<JavaClass> subclasses = new HashSet<>();
    private final Supplier<Set<JavaClass>> allSubclasses = Suppliers.memoize(new Supplier<Set<JavaClass>>() {
        @Override
        public Set<JavaClass> get() {
            Set<JavaClass> result = new HashSet<>();
            for (JavaClass subclass : subclasses) {
                result.add(subclass);
                result.addAll(subclass.getAllSubclasses());
            }
            return ImmutableSet.copyOf(result);
        }
    });
    private EnclosingDeclaration enclosingDeclaration = EnclosingDeclaration.ABSENT;
    private Optional<JavaClass> componentType = Optional.empty();
    private Map<String, JavaAnnotation<JavaClass>> annotations = emptyMap();
    private JavaClassDependencies javaClassDependencies = new JavaClassDependencies(this);  // just for stubs; will be overwritten for imported classes
    private ReverseDependencies reverseDependencies = ReverseDependencies.EMPTY;  // just for stubs; will be overwritten for imported classes
    private final CompletionProcess completionProcess;

    JavaClass(JavaClassBuilder builder) {
        source = checkNotNull(builder.getSource());
        descriptor = checkNotNull(builder.getDescriptor());
        isInterface = builder.isInterface();
        isEnum = builder.isEnum();
        isAnnotation = builder.isAnnotation();
        isRecord = builder.isRecord();
        isAnonymousClass = builder.isAnonymousClass();
        isMemberClass = builder.isMemberClass();
        modifiers = checkNotNull(builder.getModifiers());
        reflectSupplier = Suppliers.memoize(new ReflectClassSupplier());
        sourceCodeLocation = SourceCodeLocation.of(this);
        javaPackage = JavaPackage.simple(this);
        completionProcess = builder.isStub() ? CompletionProcess.stub() : CompletionProcess.start();
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
        return descriptor.getFullyQualifiedClassName();
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
        return descriptor.getSimpleClassName();
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
        return descriptor.getPackageName();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isPrimitive() {
        return descriptor.isPrimitive();
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
    public boolean isAnnotation() {
        return isAnnotation;
    }

    /**
     * Returns whether this class is a <b>record</b>
     * according to the Java Language Specification.
     * <p>
     * Records were added as preview feature with JDK 14/15
     * and were released as regular feature with JDK 16.
     * <p>
     * See also <a href="https://openjdk.java.net/jeps/395">JEP 395: Records</a>
     */
    @PublicAPI(usage = ACCESS)
    public boolean isRecord() {
        return isRecord;
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaEnumConstant> tryGetEnumConstant(String name) {
        Optional<JavaField> field = tryGetField(name);
        if (!field.isPresent() || !field.get().getModifiers().contains(ENUM)) {
            return Optional.empty();
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
        return members.getEnumConstants();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isArray() {
        return descriptor.isArray();
    }

    /**
     * This is a convenience method for {@link #tryGetComponentType()} in cases where
     * clients know that this type is certainly an array type and thus the component type present.
     * @throws IllegalStateException if this class is no array
     * @return The result of {@link #tryGetComponentType()}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getComponentType() {
        Optional<JavaClass> componentType = tryGetComponentType();
        if (!componentType.isPresent()) {
            throw new IllegalStateException(String.format("Type %s is no array", getSimpleName()));
        }
        return componentType.get();
    }

    /**
     * Returns the component type of this class, if this class is an array, otherwise
     * {@link Optional#empty()}. The component type is the type of the elements of an array type.
     * Consider {@code String[]}, then the component type would be {@code String}.
     * Likewise for {@code String[][]} the component type would be {@code String[]}.
     * @return The component type, if this type is an array, otherwise {@link Optional#empty()}
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> tryGetComponentType() {
        return componentType;
    }

    /**
     * The base component type is the class' {@link #getComponentType() component type} if it is a one-dimensional array,
     * the repeated application of {@link #getComponentType()} if it is a multi-dimensional array,
     * or the class itself if it is no array.
     * For example, the base component type of {@code int}, {@code int[]}, {@code int[][]}, ... is always {@code int}.
     * @return The base component type of this class
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getBaseComponentType() {
        JavaClass type = this;
        while (type.isArray()) {
            type = type.getComponentType();
        }
        return type;
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
        return enclosingDeclaration.isPresent();
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
     * Compare e.g. <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html">
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
     * @throws IllegalArgumentException if the class is not annotated with the given type
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
        Optional<JavaAnnotation<JavaClass>> annotation = tryGetAnnotationOfType(typeName);
        if (!annotation.isPresent()) {
            throw new IllegalArgumentException(String.format("Type %s is not annotated with @%s", getSimpleName(), typeName));
        }
        return annotation.get();
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
        return tryGetAnnotationOfType(type.getName()).map(toAnnotationOfType(type));
    }

    /**
     * Same as {@link #tryGetAnnotationOfType(Class)}, but takes the type name.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Optional<JavaAnnotation<JavaClass>> tryGetAnnotationOfType(String typeName) {
        return Optional.ofNullable(annotations.get(typeName));
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaTypeVariable<JavaClass>> getTypeParameters() {
        return typeParameters;
    }

    @PublicAPI(usage = ACCESS)
    public Set<InstanceofCheck> getInstanceofChecks() {
        return members.getInstanceofChecks();
    }

    @PublicAPI(usage = ACCESS)
    public Set<ReferencedClassObject> getReferencedClassObjects() {
        return members.getReferencedClassObjects();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass toErasure() {
        return this;
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> getRawSuperclass() {
        return superclass.getRaw();
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaType> getSuperclass() {
        return superclass.get();
    }

    /**
     * @return The complete class hierarchy, i.e. the class itself and the result of {@link #getAllRawSuperclasses()}
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getClassHierarchy() {
        return classHierarchy.get();
    }

    /**
     * @return All super classes sorted ascending by distance in the class hierarchy, i.e. first the direct super class,
     * then the super class of the super class and so on. Includes Object.class in the result.
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getAllRawSuperclasses() {
        return allRawSuperclasses.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getSubclasses() {
        return subclasses;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaType> getInterfaces() {
        return interfaces.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getRawInterfaces() {
        return interfaces.getRaw();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllRawInterfaces() {
        return allRawInterfaces.get();
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
                .addAll(getAllRawSuperclasses())
                .addAll(getAllRawInterfaces())
                .build();
    }

    /**
     * Returns the enclosing class if this class is nested within another class.
     * Otherwise {@link Optional#empty()}.<br><br>
     * Take for example
     * <pre><code>
     * class OuterClass {
     *     class InnerClass{
     *     }
     * }
     * </code></pre>
     * Then {@code InnerClass.}{@link #getEnclosingClass()} would return {@code Optional.of(OuterClass)}.
     * While {@code OuterClass.}{@link #getEnclosingClass()} would return {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaClass> getEnclosingClass() {
        return enclosingDeclaration.getEnclosingClass();
    }

    /**
     * Returns the enclosing {@link JavaCodeUnit} if this class is declared within the context of a {@link JavaCodeUnit},
     * e.g. a method or a constructor. Otherwise {@link Optional#empty()}.<br><br>
     * Take for example
     * <pre><code>
     * class OuterClass {
     *     OuterClass() {
     *         // creates a new anonymous class within the scope of the constructor
     *         new Serializable() {};
     *     }
     *
     *     void someMethod() {
     *         // creates a new local class within the scope of the method
     *         class LocalClass {}
     *     }
     *
     *     class InnerClass {}
     * }
     * </code></pre>
     * Then {@code anonymousSerializable.}{@link #getEnclosingCodeUnit()} would return the {@link JavaConstructor}
     * {@code OuterClass()} and {@code LocalClass.}{@link #getEnclosingCodeUnit()} would return the {@link JavaMethod}
     * {@code void someMethod()}.<br>
     * On the other hand {@code OuterClass.}{@link #getEnclosingCodeUnit()} or
     * {@code InnerClass.}{@link #getEnclosingCodeUnit()} would return {@link Optional#empty()}, since they are
     * not defined within the context of a {@link JavaCodeUnit}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaCodeUnit> getEnclosingCodeUnit() {
        return enclosingDeclaration.getEnclosingCodeUnit();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllSubclasses() {
        return allSubclasses.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMember> getMembers() {
        return members.get();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMember> getAllMembers() {
        return members.getAll();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getFields() {
        return members.getFields();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getAllFields() {
        return members.getAllFields();
    }

    /**
     * @return The field with the given name.
     * @throws IllegalArgumentException If this class does not have such a field.
     */
    @PublicAPI(usage = ACCESS)
    public JavaField getField(String name) {
        return members.getField(name);
    }

    /**
     * @return The field with the given name, if this class has such a field, otherwise {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaField> tryGetField(String name) {
        return members.tryGetField(name);
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnit> getCodeUnits() {
        return members.getCodeUnits();
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
        return getCodeUnitWithParameterTypeNames(name, formatNamesOf(parameters));
    }

    /**
     * Same as {@link #getCodeUnitWithParameterTypes(String, List)}, but will return {@link Optional#empty()}
     * if there is no such {@link JavaCodeUnit}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaCodeUnit> tryGetCodeUnitWithParameterTypes(String name, List<Class<?>> parameters) {
        return tryGetCodeUnitWithParameterTypeNames(name, formatNamesOf(parameters));
    }

    /**
     * @see #getCodeUnitWithParameterTypeNames(String, String...)
     */
    @PublicAPI(usage = ACCESS)
    public JavaCodeUnit getCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return members.getCodeUnitWithParameterTypeNames(name, parameters);
    }

    /**
     * Same as {@link #getCodeUnitWithParameterTypeNames(String, List)}, but will return {@link Optional#empty()}
     * if there is no such {@link JavaCodeUnit}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaCodeUnit> tryGetCodeUnitWithParameterTypeNames(String name, List<String> parameters) {
        return members.tryGetCodeUnitWithParameterTypeNames(name, parameters);
    }

    /**
     * @return The method with the given name and with zero parameters.
     * @throws IllegalArgumentException If this class does not have such a method.
     */
    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name) {
        return members.getMethod(name, Collections.<String>emptyList());
    }

    /**
     * @return The method with the given name and the given raw parameter types.
     * @throws IllegalArgumentException If this class does not have such a method.
     */
    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return members.getMethod(name, formatNamesOf(parameters));
    }

    /**
     * Same as {@link #getMethod(String, Class[])}, but with parameter signature specified as fully qualified class names.
     */
    @PublicAPI(usage = ACCESS)
    public JavaMethod getMethod(String name, String... parameters) {
        return members.getMethod(name, ImmutableList.copyOf(parameters));
    }

    /**
     * @return The method with the given name and with zero parameters,
     * if this class has such a method, otherwise {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name) {
        return members.tryGetMethod(name, Collections.<String>emptyList());
    }

    /**
     * @return The method with the given name and the given parameter types,
     * if this class has such a method, otherwise {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name, Class<?>... parameters) {
        return members.tryGetMethod(name, formatNamesOf(parameters));
    }

    /**
     * Same as {@link #tryGetMethod(String, Class[])}, but with parameter signature specified as fully qualified class names.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaMethod> tryGetMethod(String name, String... parameters) {
        return members.tryGetMethod(name, ImmutableList.copyOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethods() {
        return members.getMethods();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getAllMethods() {
        return members.getAllMethods();
    }

    /**
     * @return The constructor with zero parameters.
     * @throws IllegalArgumentException If this class does not have such a constructor.
     */
    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor() {
        return members.getConstructor(Collections.<String>emptyList());
    }

    /**
     * @return The constructor with the given parameter types. Note that these are the raw parameter types, which can
     *         include synthetic parameter types under certain circumstances. For example inner classes have the outer
     *         class as synthetic first parameter type and enums have a {@code String} name and {@code int} ordinal as
     *         prepended synthetic parameters. Unfortunately there is no fixed rule, e.g. local class constructors
     *         can have multiple synthetic parameters appended to the parameters derived from the source code.
     *
     * @throws IllegalArgumentException If this class does not have a constructor with the given parameter types.
     */
    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor(Class<?>... parameters) {
        return members.getConstructor(formatNamesOf(parameters));
    }

    /**
     * Same as {@link #getConstructor(Class[])}, but with parameter signature specified as full class names.
     */
    @PublicAPI(usage = ACCESS)
    public JavaConstructor getConstructor(String... parameters) {
        return members.getConstructor(ImmutableList.copyOf(parameters));
    }

    /**
     * @return The constructor with zero parameters,
     * if this class has such a constructor, otherwise {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaConstructor> tryGetConstructor() {
        return members.tryGetConstructor(Collections.<String>emptyList());
    }

    /**
     * @return The constructor with the given parameter types (compare {@link #getConstructor(Class[])}),
     * if this class has such a constructor, otherwise {@link Optional#empty()}.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaConstructor> tryGetConstructor(Class<?>... parameters) {
        return members.tryGetConstructor(formatNamesOf(parameters));
    }

    /**
     * Same as {@link #tryGetConstructor(Class[])}, but with parameter signature specified as fully qualified class names.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaConstructor> tryGetConstructor(String... parameters) {
        return members.tryGetConstructor(ImmutableList.copyOf(parameters));
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getConstructors() {
        return members.getConstructors();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getAllConstructors() {
        return members.getAllConstructors();
    }

    @PublicAPI(usage = ACCESS)
    public Optional<JavaStaticInitializer> getStaticInitializer() {
        return members.getStaticInitializer();
    }

    /**
     * @return All accesses of this class to any members (fields/methods/constructors)
     *
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return union(getFieldAccessesFromSelf(), getCodeUnitAccessesFromSelf());
    }

    /**
     * @return {@link #getAccessesFromSelf()} but for every class in the class hierarchy (i.e. all superclasses)
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAllAccessesFromSelf() {
        ImmutableSet.Builder<JavaAccess<?>> result = ImmutableSet.builder();
        for (JavaClass clazz : getClassHierarchy()) {
            result.addAll(clazz.getAccessesFromSelf());
        }
        return result.build();
    }

    /**
     * @return All accesses of this class to fields. These can be {@link JavaFieldAccess.AccessType#GET read} accesses
     *         (e.g. {@code return this.example}) or {@link JavaFieldAccess.AccessType#SET write} accesses
     *         (e.g. {@code this.example = example})
     *
     * @see #getAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccessesFromSelf() {
        return members.getFieldAccessesFromSelf();
    }

    /**
     * @return All access of this class to other code units. This can be calls to methods/constructors (e.g. {@code someExample.call()})
     *         or references of methods/constructors (e.g. {@code SomeExample::call})
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitAccess<?>> getCodeUnitAccessesFromSelf() {
        return union(getCodeUnitCallsFromSelf(), getCodeUnitReferencesFromSelf());
    }

    /**
     * @deprecated Use {@link #getCodeUnitCallsFromSelf()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCallsFromSelf() {
        return getCodeUnitCallsFromSelf();
    }

    /**
     * Returns all calls of this class to methods or constructors.
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCodeUnitCallsFromSelf() {
        return union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    /**
     * @return All method calls (e.g. a call to {@code SomeExample.someMethod()})
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        return members.getMethodCallsFromSelf();
    }

    /**
     * @return All constructor calls (e.g. a call to {@code SomeExample()})
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        return members.getConstructorCallsFromSelf();
    }

    /**
     * @return All references of this class to {@link #getMethodReferencesFromSelf() method} or {@link #getConstructorReferencesFromSelf() constructor references}.
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getConstructorReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitReference<?>> getCodeUnitReferencesFromSelf() {
        return union(getMethodReferencesFromSelf(), getConstructorReferencesFromSelf());
    }

    /**
     * @return All method references (e.g. {@code SomeExample::someMethod})
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getConstructorReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodReference> getMethodReferencesFromSelf() {
        return members.getMethodReferencesFromSelf();
    }

    /**
     * @return All constructor references (e.g. {@code SomeExample::new})
     *
     * @see #getAccessesFromSelf()
     * @see #getFieldAccessesFromSelf()
     * @see #getCodeUnitAccessesFromSelf()
     * @see #getCodeUnitCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     * @see #getMethodCallsFromSelf()
     * @see #getCodeUnitReferencesFromSelf()
     * @see #getMethodReferencesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorReference> getConstructorReferencesFromSelf() {
        return members.getConstructorReferencesFromSelf();
    }

    /**
     * @return All dependencies originating directly from this class (i.e. where this class is the origin).
     *         For further details about dependencies refer to {@link Dependency}.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesFromSelf() {
        return javaClassDependencies.getDirectDependenciesFromClass();
    }

    /**
     * Returns the transitive closure of all dependencies originating from this class, i.e. its direct dependencies
     * and the dependencies from all imported target classes.
     * @return all transitive dependencies (including direct dependencies) from this class
     * @see #getDirectDependenciesFromSelf()
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getTransitiveDependenciesFromSelf() {
        return JavaClassTransitiveDependencies.findTransitiveDependenciesFrom(this);
    }

    /**
     * Like {@link #getDirectDependenciesFromSelf()}, but instead returns all dependencies where this class
     * is target.
     *
     * @return Dependencies where this class is the target.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDirectDependenciesToSelf() {
        return reverseDependencies.getDirectDependenciesTo(this);
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccessesToSelf() {
        return members.getFieldAccessesToSelf();
    }

    /**
     * Like {@link #getCodeUnitAccessesFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitAccess<?>> getCodeUnitAccessesToSelf() {
        return union(getCodeUnitCallsToSelf(), getCodeUnitReferencesToSelf());
    }

    /**
     * Like {@link #getCodeUnitCallsFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCodeUnitCallsToSelf() {
        return union(getMethodCallsToSelf(), getConstructorCallsToSelf());
    }

    /**
     * Like {@link #getCodeUnitReferencesFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitReference<?>> getCodeUnitReferencesToSelf() {
        return union(getMethodReferencesToSelf(), getConstructorReferencesToSelf());
    }

    /**
     * Like {@link #getMethodCallsFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsToSelf() {
        return members.getMethodCallsToSelf();
    }

    /**
     * Like {@link #getMethodReferencesFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodReference> getMethodReferencesToSelf() {
        return members.getMethodReferencesToSelf();
    }

    /**
     * Like {@link #getConstructorCallsFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsToSelf() {
        return members.getConstructorCallsToSelf();
    }

    /**
     * Like {@link #getConstructorReferencesFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorReference> getConstructorReferencesToSelf() {
        return members.getConstructorReferencesToSelf();
    }

    /**
     * Like {@link #getAccessesFromSelf()} but this class is target instead of origin.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesToSelf() {
        return union(getFieldAccessesToSelf(), getCodeUnitAccessesToSelf());
    }

    /**
     * @return Fields of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaField> getFieldsWithTypeOfSelf() {
        return reverseDependencies.getFieldsWithTypeOf(this);
    }

    /**
     * @return Methods of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithParameterTypeOfSelf() {
        return reverseDependencies.getMethodsWithParameterTypeOf(this);
    }

    /**
     * @return Methods of all imported classes that have a return type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaMethod> getMethodsWithReturnTypeOfSelf() {
        return reverseDependencies.getMethodsWithReturnTypeOf(this);
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsWithTypeOfSelf() {
        return reverseDependencies.getMethodThrowsDeclarationsWithTypeOf(this);
    }

    /**
     * @return Constructors of all imported classes that have a parameter type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructor> getConstructorsWithParameterTypeOfSelf() {
        return reverseDependencies.getConstructorsWithParameterTypeOf(this);
    }

    /**
     * @return {@link ThrowsDeclaration ThrowsDeclarations} of all imported classes that have the type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<JavaConstructor>> getConstructorsWithThrowsDeclarationTypeOfSelf() {
        return reverseDependencies.getConstructorsWithThrowsDeclarationTypeOf(this);
    }

    /**
     * @return All imported {@link JavaAnnotation JavaAnnotations} that have the annotation type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<?>> getAnnotationsWithTypeOfSelf() {
        return reverseDependencies.getAnnotationsWithTypeOf(this);
    }

    /**
     * @return All imported {@link JavaAnnotation JavaAnnotations} that have a parameter with type of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaAnnotation<?>> getAnnotationsWithParameterTypeOfSelf() {
        return reverseDependencies.getAnnotationsWithParameterTypeOf(this);
    }

    /**
     * @return All imported {@link InstanceofCheck InstanceofChecks} that check if another class is an instance of this class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<InstanceofCheck> getInstanceofChecksWithTypeOfSelf() {
        return reverseDependencies.getInstanceofChecksWithTypeOf(this);
    }

    /**
     * @return Whether this class has been fully imported, including all dependencies.<br>
     *         Classes that are only transitively imported are not necessarily fully imported.<br><br>
     *         Suppose you only import a class {@code Foo} that calls a method of class {@code Bar}.
     *         Then {@code Bar} is, as a dependency of the fully imported class {@code Foo}, only transitively imported.
     */
    @PublicAPI(usage = ACCESS)
    public boolean isFullyImported() {
        return completionProcess.hasFinished();
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
                .add(this).addAll(getAllSubclasses()).build();

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
                .addAll(getClassHierarchy()).addAll(getAllRawInterfaces()).build();

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
        completeSuperclassFrom(context);
        completeInterfacesFrom(context);
        completionProcess.markClassHierarchyComplete();
    }

    private void completeSuperclassFrom(ImportContext context) {
        Optional<JavaClass> rawSuperclass = context.createSuperclass(this);
        if (rawSuperclass.isPresent()) {
            rawSuperclass.get().subclasses.add(this);
            this.superclass = this.superclass.withRawType(rawSuperclass.get());
        }
    }

    private void completeInterfacesFrom(ImportContext context) {
        List<JavaClass> rawInterfaces = context.createInterfaces(this);
        for (JavaClass i : rawInterfaces) {
            i.subclasses.add(this);
        }
        this.interfaces = this.interfaces.withRawTypes(rawInterfaces);
    }

    void completeEnclosingDeclarationFrom(ImportContext context) {
        enclosingDeclaration = createEnclosingDeclaration(context);
        completionProcess.markEnclosingDeclarationComplete();
    }

    private EnclosingDeclaration createEnclosingDeclaration(ImportContext context) {
        Optional<JavaCodeUnit> enclosingCodeUnit = context.createEnclosingCodeUnit(this);
        return enclosingCodeUnit.isPresent()
                ? EnclosingDeclaration.ofCodeUnit(enclosingCodeUnit.get())
                : EnclosingDeclaration.ofClass(context.createEnclosingClass(this));
    }

    void completeTypeParametersFrom(ImportContext context) {
        typeParameters = context.createTypeParameters(this);
        completionProcess.markTypeParametersComplete();
    }

    void completeGenericSuperclassFrom(ImportContext context) {
        Optional<JavaType> genericSuperclass = context.createGenericSuperclass(this);
        if (genericSuperclass.isPresent()) {
            superclass = superclass.withGenericType(genericSuperclass.get());
        }
        completionProcess.markGenericSuperclassComplete();
    }

    void completeGenericInterfacesFrom(ImportContext context) {
        Optional<List<JavaType>> genericInterfaces = context.createGenericInterfaces(this);
        if (genericInterfaces.isPresent()) {
            interfaces = interfaces.withGenericTypes(genericInterfaces.get());
        }
        completionProcess.markGenericInterfacesComplete();
    }

    void completeMembers(final ImportContext context) {
        members = JavaClassMembers.create(this, context);
        completionProcess.markMembersComplete();
    }

    void completeAnnotations(final ImportContext context) {
        annotations = context.createAnnotations(this);
        members.completeAnnotations(context);
        completionProcess.markAnnotationsComplete();
    }

    JavaClassDependencies completeFrom(ImportContext context) {
        completeComponentType(context);
        members.completeAccessesFrom(context);
        javaClassDependencies = new JavaClassDependencies(this);
        return javaClassDependencies;
    }

    private void completeComponentType(ImportContext context) {
        JavaClass current = this;
        while (current.isArray() && !current.componentType.isPresent()) {
            JavaClass componentType = context.resolveClass(current.descriptor.tryGetComponentType().get().getFullyQualifiedClassName());
            current.componentType = Optional.of(componentType);
            current = componentType;
        }
    }

    void setReverseDependencies(ReverseDependencies reverseDependencies) {
        this.reverseDependencies = reverseDependencies;
        members.setReverseDependencies(reverseDependencies);
        completionProcess.markDependenciesComplete();
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + descriptor.getFullyQualifiedClassName() + "'}";
    }

    /**
     * @deprecated use {@link Formatters#formatNamesOf(Class[])} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(Class<?>... paramTypes) {
        return formatNamesOf(paramTypes);
    }

    /**
     * @deprecated use {@link Formatters#formatNamesOf(Iterable)} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public static List<String> namesOf(Iterable<Class<?>> paramTypes) {
        return formatNamesOf(paramTypes);
    }

    /**
     * @return all throws declarations on any method or constructor of this class
     *         (e.g. {@code void method() throws SomeException})
     */
    @PublicAPI(usage = ACCESS)
    public Set<ThrowsDeclaration<? extends JavaCodeUnit>> getThrowsDeclarations() {
        ImmutableSet.Builder<ThrowsDeclaration<? extends JavaCodeUnit>> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : getCodeUnits()) {
            result.addAll(codeUnit.getThrowsClause());
        }
        return result.build();
    }

    private static class Superclass {
        private static final Superclass ABSENT = new Superclass(Optional.<JavaType>empty());

        private final Optional<JavaClass> rawType;
        private final Optional<JavaType> type;

        private Superclass(JavaType type) {
            this(Optional.of(type));
        }

        private Superclass(Optional<JavaType> type) {
            this.rawType = type.map(TO_ERASURE);
            this.type = type;
        }

        Optional<JavaClass> getRaw() {
            return rawType;
        }

        Optional<JavaType> get() {
            return type.or(rawType);
        }

        Set<JavaClass> getAllRawInterfaces() {
            return rawType.isPresent() ? rawType.get().getAllRawInterfaces() : Collections.<JavaClass>emptySet();
        }

        Superclass withRawType(JavaClass newRawType) {
            return new Superclass(newRawType);
        }

        Superclass withGenericType(JavaType newGenericType) {
            return new Superclass(newGenericType);
        }
    }

    private static class Interfaces {
        static final Interfaces EMPTY = new Interfaces(ImmutableSet.<JavaClass>of(), ImmutableSet.<JavaType>of());

        private final ImmutableSet<JavaClass> rawTypes;
        private final ImmutableSet<JavaType> types;

        private Interfaces(ImmutableSet<JavaClass> rawTypes, ImmutableSet<JavaType> types) {
            this.rawTypes = checkNotNull(rawTypes);
            this.types = checkNotNull(types);
        }

        ImmutableSet<JavaClass> getRaw() {
            return rawTypes;
        }

        ImmutableSet<JavaType> get() {
            return types.isEmpty() ? getRawTypesAsJavaTypes() : types;
        }

        @SuppressWarnings({"unchecked", "rawtypes"}) // ImmutableSet is a pure producer, thus covariant, making the cast safe
        private ImmutableSet<JavaType> getRawTypesAsJavaTypes() {
            return (ImmutableSet) getRaw();
        }

        Interfaces withRawTypes(List<JavaClass> rawTypes) {
            return new Interfaces(ImmutableSet.copyOf(rawTypes), types);
        }

        Interfaces withGenericTypes(List<JavaType> genericTypes) {
            return new Interfaces(rawTypes, ImmutableSet.copyOf(genericTypes));
        }
    }

    private static class EnclosingDeclaration {
        static final EnclosingDeclaration ABSENT = new EnclosingDeclaration(Optional.<JavaCodeUnit>empty(), Optional.<JavaClass>empty());

        private final Optional<JavaCodeUnit> enclosingCodeUnit;
        private final Optional<JavaClass> enclosingClass;

        private EnclosingDeclaration(Optional<JavaCodeUnit> enclosingCodeUnit, Optional<JavaClass> enclosingClass) {
            this.enclosingCodeUnit = checkNotNull(enclosingCodeUnit);
            this.enclosingClass = checkNotNull(enclosingClass);
        }

        boolean isPresent() {
            return enclosingClass.isPresent();
        }

        Optional<JavaClass> getEnclosingClass() {
            return enclosingClass;
        }

        Optional<JavaCodeUnit> getEnclosingCodeUnit() {
            return enclosingCodeUnit;
        }

        static EnclosingDeclaration ofCodeUnit(JavaCodeUnit codeUnit) {
            return new EnclosingDeclaration(Optional.of(codeUnit), Optional.of(codeUnit.getOwner()));
        }

        static EnclosingDeclaration ofClass(Optional<JavaClass> clazz) {
            return new EnclosingDeclaration(Optional.<JavaCodeUnit>empty(), clazz);
        }
    }

    private abstract static class CompletionProcess {
        private static final CompletionProcess stubCompletionProcess = new CompletionProcess() {
            @Override
            boolean hasFinished() {
                return false;
            }

            @Override
            public void markClassHierarchyComplete() {
            }

            @Override
            public void markEnclosingDeclarationComplete() {
            }

            @Override
            public void markTypeParametersComplete() {
            }

            @Override
            public void markGenericSuperclassComplete() {
            }

            @Override
            public void markGenericInterfacesComplete() {
            }

            @Override
            public void markMembersComplete() {
            }

            @Override
            public void markAnnotationsComplete() {
            }

            @Override
            public void markDependenciesComplete() {
            }
        };

        abstract boolean hasFinished();

        public abstract void markClassHierarchyComplete();

        public abstract void markEnclosingDeclarationComplete();

        public abstract void markTypeParametersComplete();

        public abstract void markGenericSuperclassComplete();

        public abstract void markGenericInterfacesComplete();

        public abstract void markMembersComplete();

        public abstract void markAnnotationsComplete();

        public abstract void markDependenciesComplete();

        static CompletionProcess start() {
            return new FullCompletionProcess();
        }

        static CompletionProcess stub() {
            return stubCompletionProcess;
        }
    }

    private static class FullCompletionProcess extends CompletionProcess {
        private boolean classHierarchyComplete = false;
        private boolean enclosingDeclarationComplete = false;
        private boolean typeParametersComplete = false;
        private boolean genericSuperclassComplete = false;
        private boolean genericInterfacesComplete = false;
        private boolean membersComplete = false;
        private boolean annotationsComplete = false;
        private boolean dependenciesComplete = false;

        @Override
        boolean hasFinished() {
            return classHierarchyComplete
                    && enclosingDeclarationComplete
                    && typeParametersComplete
                    && genericSuperclassComplete
                    && genericInterfacesComplete
                    && membersComplete
                    && annotationsComplete
                    && dependenciesComplete;
        }

        @Override
        public void markClassHierarchyComplete() {
            this.classHierarchyComplete = true;
        }

        @Override
        public void markEnclosingDeclarationComplete() {
            this.enclosingDeclarationComplete = true;
        }

        @Override
        public void markTypeParametersComplete() {
            this.typeParametersComplete = true;
        }

        @Override
        public void markGenericSuperclassComplete() {
            this.genericSuperclassComplete = true;
        }

        @Override
        public void markGenericInterfacesComplete() {
            this.genericInterfacesComplete = true;
        }

        @Override
        public void markMembersComplete() {
            this.membersComplete = true;
        }

        @Override
        public void markAnnotationsComplete() {
            this.annotationsComplete = true;
        }

        @Override
        public void markDependenciesComplete() {
            this.dependenciesComplete = true;
        }
    }

    public static final class Functions {
        private Functions() {
        }

        /**
         * @see #getSimpleName()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, String> GET_SIMPLE_NAME = new ChainableFunction<JavaClass, String>() {
            @Override
            public String apply(JavaClass input) {
                return input.getSimpleName();
            }
        };

        /**
         * @see #getPackageName()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, String> GET_PACKAGE_NAME = new ChainableFunction<JavaClass, String>() {
            @Override
            public String apply(JavaClass input) {
                return input.getPackageName();
            }
        };

        /**
         * @see #getPackage()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, JavaPackage> GET_PACKAGE = new ChainableFunction<JavaClass, JavaPackage>() {
            @Override
            public JavaPackage apply(JavaClass input) {
                return input.getPackage();
            }
        };

        /**
         * @see #getMembers()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMember>> GET_MEMBERS = new ChainableFunction<JavaClass, Set<JavaMember>>() {
            @Override
            public Set<JavaMember> apply(JavaClass input) {
                return input.getMembers();
            }
        };

        /**
         * @see #getFields()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaField>> GET_FIELDS = new ChainableFunction<JavaClass, Set<JavaField>>() {
            @Override
            public Set<JavaField> apply(JavaClass input) {
                return input.getFields();
            }
        };

        /**
         * @see #getCodeUnits()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCodeUnit>> GET_CODE_UNITS =
                new ChainableFunction<JavaClass, Set<JavaCodeUnit>>() {
                    @Override
                    public Set<JavaCodeUnit> apply(JavaClass input) {
                        return input.getCodeUnits();
                    }
                };

        /**
         * @see #getMethods()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethod>> GET_METHODS = new ChainableFunction<JavaClass, Set<JavaMethod>>() {
            @Override
            public Set<JavaMethod> apply(JavaClass input) {
                return input.getMethods();
            }
        };

        /**
         * @see #getConstructors()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructor>> GET_CONSTRUCTORS =
                new ChainableFunction<JavaClass, Set<JavaConstructor>>() {
                    @Override
                    public Set<JavaConstructor> apply(JavaClass input) {
                        return input.getConstructors();
                    }
                };

        /**
         * @see #getStaticInitializer()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Optional<JavaStaticInitializer>> GET_STATIC_INITIALIZER =
                new ChainableFunction<JavaClass, Optional<JavaStaticInitializer>>() {
                    @Override
                    public Optional<JavaStaticInitializer> apply(JavaClass input) {
                        return input.getStaticInitializer();
                    }
                };

        /**
         * @see #getFieldAccessesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaFieldAccess>> GET_FIELD_ACCESSES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaFieldAccess>>() {
                    @Override
                    public Set<JavaFieldAccess> apply(JavaClass input) {
                        return input.getFieldAccessesFromSelf();
                    }
                };

        /**
         * @see #getMethodCallsFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethodCall>> GET_METHOD_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaMethodCall>>() {
                    @Override
                    public Set<JavaMethodCall> apply(JavaClass input) {
                        return input.getMethodCallsFromSelf();
                    }
                };

        /**
         * @see #getConstructorCallsFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructorCall>> GET_CONSTRUCTOR_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaConstructorCall>>() {
                    @Override
                    public Set<JavaConstructorCall> apply(JavaClass input) {
                        return input.getConstructorCallsFromSelf();
                    }
                };

        /**
         * @see #getCodeUnitCallsFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCall<?>>> GET_CODE_UNIT_CALLS_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaCall<?>>>() {
                    @Override
                    public Set<JavaCall<?>> apply(JavaClass input) {
                        return input.getCodeUnitCallsFromSelf();
                    }
                };

        /**
         * @deprecated Use {@link #GET_CODE_UNIT_CALLS_FROM_SELF} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCall<?>>> GET_CALLS_FROM_SELF = GET_CODE_UNIT_CALLS_FROM_SELF;

        /**
         * @see #getMethodReferencesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethodReference>> GET_METHOD_REFERENCES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaMethodReference>>() {
                    @Override
                    public Set<JavaMethodReference> apply(JavaClass input) {
                        return input.getMethodReferencesFromSelf();
                    }
                };

        /**
         * @see #getConstructorReferencesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructorReference>> GET_CONSTRUCTOR_REFERENCES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaConstructorReference>>() {
                    @Override
                    public Set<JavaConstructorReference> apply(JavaClass input) {
                        return input.getConstructorReferencesFromSelf();
                    }
                };

        /**
         * @see #getCodeUnitReferencesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCodeUnitReference<?>>> GET_CODE_UNIT_REFERENCES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaCodeUnitReference<?>>>() {
                    @Override
                    public Set<JavaCodeUnitReference<?>> apply(JavaClass input) {
                        return input.getCodeUnitReferencesFromSelf();
                    }
                };

        /**
         * @see #getMethodReferencesToSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaMethodReference>> GET_METHOD_REFERENCES_TO_SELF =
                new ChainableFunction<JavaClass, Set<JavaMethodReference>>() {
                    @Override
                    public Set<JavaMethodReference> apply(JavaClass input) {
                        return input.getMethodReferencesToSelf();
                    }
                };

        /**
         * @see #getConstructorReferencesToSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaConstructorReference>> GET_CONSTRUCTOR_REFERENCES_TO_SELF =
                new ChainableFunction<JavaClass, Set<JavaConstructorReference>>() {
                    @Override
                    public Set<JavaConstructorReference> apply(JavaClass input) {
                        return input.getConstructorReferencesToSelf();
                    }
                };

        /**
         * @see #getCodeUnitReferencesToSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaCodeUnitReference<?>>> GET_CODE_UNIT_REFERENCES_TO_SELF =
                new ChainableFunction<JavaClass, Set<JavaCodeUnitReference<?>>>() {
                    @Override
                    public Set<JavaCodeUnitReference<?>> apply(JavaClass input) {
                        return input.getCodeUnitReferencesToSelf();
                    }
                };

        /**
         * @see #getAccessesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaAccess<?>>> GET_ACCESSES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<JavaAccess<?>>>() {
                    @Override
                    public Set<JavaAccess<?>> apply(JavaClass input) {
                        return input.getAccessesFromSelf();
                    }
                };

        /**
         * @see #getDirectDependenciesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<Dependency>> GET_DIRECT_DEPENDENCIES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<Dependency>>() {
                    @Override
                    public Set<Dependency> apply(JavaClass input) {
                        return input.getDirectDependenciesFromSelf();
                    }
                };

        /**
         * @see #getTransitiveDependenciesFromSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<Dependency>> GET_TRANSITIVE_DEPENDENCIES_FROM_SELF =
                new ChainableFunction<JavaClass, Set<Dependency>>() {
                    @Override
                    public Set<Dependency> apply(JavaClass input) {
                        return input.getTransitiveDependenciesFromSelf();
                    }
                };

        /**
         * @see #getAccessesToSelf()
         */
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaClass, Set<JavaAccess<?>>> GET_ACCESSES_TO_SELF =
                new ChainableFunction<JavaClass, Set<JavaAccess<?>>>() {
                    @Override
                    public Set<JavaAccess<?>> apply(JavaClass input) {
                        return input.getAccessesToSelf();
                    }
                };

        /**
         * @see #getDirectDependenciesToSelf()
         */
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
        public static final DescribedPredicate<JavaClass> ANNOTATIONS = new DescribedPredicate<JavaClass>("annotations") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnnotation();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final DescribedPredicate<JavaClass> RECORDS = new DescribedPredicate<JavaClass>("records") {
            @Override
            public boolean apply(JavaClass input) {
                return input.isRecord();
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

        /**
         * @param type the type to check for assignability
         * @return a {@link DescribedPredicate} that returns {@code true}, if the respective {@link JavaClass}
         *         is assignable to the supplied {@code type}. I.e. the type represented by the tested {@link JavaClass}
         *         could be casted to the supplied {@code type}.<br>
         *         This is the opposite of {@link #assignableFrom(Class)}:
         *         some class {@code A} is assignable to a class {@code B} if and only if {@code B} is assignable from {@code A}.
         *
         * @see #assignableTo(String)
         * @see #assignableTo(DescribedPredicate)
         * @see #assignableFrom(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final Class<?> type) {
            return assignableTo(type.getName());
        }

        /**
         * @param type the type to check for assignability
         * @return a {@link DescribedPredicate} that returns {@code true}, if the respective {@link JavaClass}
         *         is assignable from the supplied {@code type}. I.e. the supplied {@code type}
         *         could be casted to the type represented by the tested {@link JavaClass}.<br>
         *         This is the opposite of {@link #assignableTo(Class)}:
         *         some class {@code B} is assignable from a class {@code A} if and only if {@code A} is assignable to {@code B}.
         *
         * @see #assignableFrom(String)
         * @see #assignableFrom(DescribedPredicate)
         * @see #assignableTo(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final Class<?> type) {
            return assignableFrom(type.getName());
        }

        /**
         * Same as {@link #assignableTo(Class)} but takes a fully qualified class name as an argument instead of a class object.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final String typeName) {
            return assignableTo(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        /**
         * Same as {@link #assignableFrom(Class)} but takes a fully qualified class name as an argument instead of a class object.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final String typeName) {
            return assignableFrom(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        /**
         * Same as {@link #assignableTo(Class)}, but returns {@code true} whenever the tested {@link JavaClass}
         * is assignable to a class that matches the supplied predicate.<br>
         * This is the opposite of {@link #assignableFrom(DescribedPredicate)}:
         * some class {@code A} is assignable to a class {@code B} if and only if {@code B} is assignable from {@code A}.
         *
         * @see #assignableTo(Class)
         * @see #assignableTo(String)
         * @see #assignableFrom(DescribedPredicate)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableTo(final DescribedPredicate<? super JavaClass> predicate) {
            return new AssignableToPredicate(predicate);
        }

        /**
         * Same as {@link #assignableFrom(Class)}, but returns {@code true} whenever the tested {@link JavaClass}
         * is assignable from a class that matches the supplied predicate.<br>
         * This is the opposite of {@link #assignableTo(DescribedPredicate)}:
         * some class {@code B} is assignable from a class {@code A} if and only if {@code A} is assignable to {@code A}.
         *
         * @see #assignableFrom(Class)
         * @see #assignableFrom(String)
         * @see #assignableTo(DescribedPredicate)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> assignableFrom(final DescribedPredicate<? super JavaClass> predicate) {
            return new AssignableFromPredicate(predicate);
        }

        /**
         * @param type the interface type to check for
         * @return a {@link DescribedPredicate} that returns {@code true} if the tested {@link JavaClass} implements the supplied
         *         interface {@code type}. I.e. the supplied {@code type} must be an interface and the tested {@link JavaClass}
         *         must be a class (it resembles delarations like {@code class A implements B},
         *         which only works for a class {@code A} and an interface {@code B}).
         * @throws InvalidSyntaxUsageException if {@code type} is not an interface
         *
         * @see #implement(String)
         * @see #implement(DescribedPredicate)
         * @see #assignableTo(Class)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final Class<?> type) {
            if (!type.isInterface()) {
                throw new InvalidSyntaxUsageException(String.format(
                        "implement(type) can only ever be true, if type is an interface, but type %s is not. "
                                + "Do you maybe want to use the more generic assignableTo(type)?", type.getName()));
            }
            return implement(type.getName());
        }

        /**
         * Same as {@link #implement(Class)} but takes a fully qualified class name as an argument instead of a class object.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final String typeName) {
            return implement(GET_NAME.is(equalTo(typeName)).as(typeName));
        }

        /**
         * Same as {@link #implement(Class)} but returns {@code true} whenever the tested {@link JavaClass} implements
         * an interface that matches the supplied predicate.
         *
         * @see #implement(Class)
         * @see #implement(String)
         * @see #assignableTo(DescribedPredicate)
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> implement(final DescribedPredicate<? super JavaClass> predicate) {
            DescribedPredicate<JavaClass> selfIsImplementation = not(INTERFACES);
            DescribedPredicate<JavaClass> interfacePredicate = predicate.<JavaClass>forSubtype().and(INTERFACES);
            return selfIsImplementation.and(assignableTo(interfacePredicate))
                    .as("implement " + predicate.getDescription());
        }

        /**
         * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
         * characters, '..' stands for any sequence of packages.
         * For further details see {@link PackageMatcher}.
         *
         * @param packageIdentifier A string representing the identifier to match packages against
         * @return A {@link DescribedPredicate} returning {@code true} if and only if the package of the
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

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaMember members} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} members.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one member matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyMembersThat(DescribedPredicate<? super JavaMember> predicate) {
            return new ContainAnyMembersThatPredicate<>("members", GET_MEMBERS, predicate);
        }

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaField fields} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} fields.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one field matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyFieldsThat(DescribedPredicate<? super JavaField> predicate) {
            return new ContainAnyMembersThatPredicate<>("fields", GET_FIELDS, predicate);
        }

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaCodeUnit code units} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} code units.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one code unit matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyCodeUnitsThat(DescribedPredicate<? super JavaCodeUnit> predicate) {
            return new ContainAnyMembersThatPredicate<>("code units", GET_CODE_UNITS, predicate);
        }

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaMethod methods} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} methods.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one method matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyMethodsThat(DescribedPredicate<? super JavaMethod> predicate) {
            return new ContainAnyMembersThatPredicate<>("methods", GET_METHODS, predicate);
        }

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaConstructor constructors} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} constructors.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one constructor matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyConstructorsThat(DescribedPredicate<? super JavaConstructor> predicate) {
            return new ContainAnyMembersThatPredicate<>("constructors", GET_CONSTRUCTORS, predicate);
        }

        /**
         * A predicate to determine if a {@link JavaClass} contains one or more {@link JavaConstructor constructors} matching the supplied predicate.
         *
         * @param predicate The predicate to check against the {@link JavaClass classes'} constructors.
         * @return A {@link DescribedPredicate} returning true, if and only if the tested {@link JavaClass} contains at least
         * one constructor matching the given predicate.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaClass> containAnyStaticInitializersThat(DescribedPredicate<? super JavaStaticInitializer> predicate) {
            return new ContainAnyMembersThatPredicate<>("static initializers", GET_STATIC_INITIALIZER.then(AS_SET), predicate);
        }

        private static final Function<Optional<JavaStaticInitializer>, Set<JavaStaticInitializer>> AS_SET = new Function<Optional<JavaStaticInitializer>, Set<JavaStaticInitializer>>() {
            @Override
            public Set<JavaStaticInitializer> apply(Optional<JavaStaticInitializer> input) {
                return input.asSet();
            }
        };

        private static class BelongToAnyOfPredicate extends DescribedPredicate<JavaClass> {
            private final Class<?>[] classes;

            BelongToAnyOfPredicate(Class<?>... classes) {
                super("belong to any of " + formatNamesOf(classes));
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

        private static class ContainAnyMembersThatPredicate<T extends JavaMember> extends DescribedPredicate<JavaClass> {
            private final Function<JavaClass, Set<T>> getMembers;
            private final DescribedPredicate<? super T> predicate;

            ContainAnyMembersThatPredicate(String memberDescription, Function<JavaClass, Set<T>> getMembers, DescribedPredicate<? super T> predicate) {
                super("contain any " + memberDescription + " that " + predicate.getDescription());
                this.getMembers = getMembers;
                this.predicate = predicate;
            }

            @Override
            public boolean apply(JavaClass input) {
                for (T member : getMembers.apply(input)) {
                    if (predicate.apply(member)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    private class ReflectClassSupplier implements Supplier<Class<?>> {
        @Override
        public Class<?> get() {
            return descriptor.resolveClass(getCurrentClassLoader(getClass()));
        }
    }
}
