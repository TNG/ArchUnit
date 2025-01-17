/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * Represents a package of Java classes as defined by the
 * <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>.
 * I.e. a namespace/group for related classes, where each package can also contain further subpackages.
 * Thus, packages define a hierarchical tree-like structure.<br>
 * An example would be the package {@code java.lang} which contains {@code java.lang.Object}.<br>
 * ArchUnit will consider the "classes of a package" to be the classes residing <b>directly</b>
 * within the package. Furthermore, "subpackages" of a package are considered packages that are residing
 * <b>directly</b> within this package. On the contrary, ArchUnit will call the hierarchical tree-like structure
 * consisting of all packages that can be reached by traversing subpackages, subpackages of subpackages, etc.,
 * as "package tree".
 * <br><br>
 * Take for example the classes
 * <pre><code>
 * com.example.TopLevel
 * com.example.first.First
 * com.example.first.nested.FirstNested
 * com.example.first.nested.deeper_nested.FirstDeeperNested
 * com.example.second.Second
 * com.example.second.nested.SecondNested
 * </code></pre>
 * Then the package {@code com.example} would contain only the class {@code com.example.TopLevel}. It would also
 * contain two subpackages {@code com.example.first} and {@code com.example.second} (but not {@code com.example.first.nested}
 * as that is not <b>directly</b> contained within {@code com.example}).<br>
 * The package tree of {@code com.example} would contain all packages (and classes within)
 * <pre><code>
 * {@code com.example}
 * {@code com.example.first}
 * {@code com.example.first.nested}
 * {@code com.example.first.nested.deeper_nested}
 * {@code com.example.second}
 * {@code com.example.second.nested}
 * </code></pre>
 */
@PublicAPI(usage = ACCESS)
public final class JavaPackage implements HasName, HasAnnotations<JavaPackage> {
    private final String name;
    private final String relativeName;
    private final Set<JavaClass> classes;
    private final Optional<JavaClass> packageInfo;
    private final Map<String, JavaPackage> subpackages;
    private Optional<JavaPackage> parent = Optional.empty();

    private JavaPackage(String name, Set<JavaClass> classes, Map<String, JavaPackage> subpackages) {
        this.name = checkNotNull(name);
        relativeName = name.substring(name.lastIndexOf(".") + 1);
        this.classes = ImmutableSet.copyOf(classes);
        this.packageInfo = tryGetClassWithSimpleName("package-info");
        this.subpackages = ImmutableMap.copyOf(subpackages);
    }

    /**
     * @return the (full) name of this package, e.g. {@code com.mycompany.myapp}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    /**
     * @return the (relative) name of this package, e.g. {@code lang} for package {@code java.lang}
     */
    @PublicAPI(usage = ACCESS)
    public String getRelativeName() {
        return relativeName;
    }

    /**
     * @return The {@link JavaClass} representing the compiled {@code package-info.class} file of this {@link JavaPackage}
     *         (for details refer to the <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>).
     *         Will throw an {@link IllegalArgumentException} if no {@code package-info} exists in this package.
     */
    @PublicAPI(usage = ACCESS)
    public HasAnnotations<?> getPackageInfo() {
        Optional<? extends HasAnnotations<?>> packageInfo = tryGetPackageInfo();
        if (!packageInfo.isPresent()) {
            throw new IllegalArgumentException(String.format("%s does not contain a package-info.java", getDescription()));
        }
        return packageInfo.get();
    }

    /**
     * @return The {@link JavaClass} representing the compiled {@code package-info.class} file of this {@link JavaPackage}
     *         or {@link Optional#empty()} if no {@code package-info} exists in this package
     *         (for details refer to the <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>).
     */
    @PublicAPI(usage = ACCESS)
    public Optional<? extends HasAnnotations<?>> tryGetPackageInfo() {
        return packageInfo;
    }

    /**
     * @return All annotations on the compiled {@link #getPackageInfo() package-info.class} file
     *         (for details refer to the <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>).
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Set<? extends JavaAnnotation<JavaPackage>> getAnnotations() {
        return packageInfo
                .map(it -> it.getAnnotations().stream().map(withSelfAsOwner).collect(toSet()))
                .orElse(emptySet());
    }

    /**
     * @return The {@link Annotation} of the given type on the {@link #getPackageInfo() package-info.class} of this package
     *         (for details refer to the <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>).
     *         Will throw an {@link IllegalArgumentException} if either there is no {@code package-info}
     *         or the {@code package-info} is not annotated with the respective annotation type.
     * @param <A> The type of the {@link Annotation} to retrieve
     * @see #tryGetAnnotationOfType(Class)
     * @see #getAnnotationOfType(String)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> A getAnnotationOfType(Class<A> type) {
        return getAnnotationOfType(type.getName()).as(type);
    }

    /**
     * @return The {@link JavaAnnotation} matching the given type on the {@link #getPackageInfo() package-info.class} of this package
     *         (for details refer to the <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-7.html">Java Language Specification</a>).
     *         Will throw an {@link IllegalArgumentException} if either there is no {@code package-info}
     *         or the {@code package-info} is not annotated with the respective annotation type.
     * @see #tryGetAnnotationOfType(String)
     * @see #getAnnotationOfType(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public JavaAnnotation<JavaPackage> getAnnotationOfType(String typeName) {
        Optional<JavaAnnotation<JavaPackage>> annotation = tryGetAnnotationOfType(typeName);
        if (!annotation.isPresent()) {
            throw new IllegalArgumentException(String.format("%s is not annotated with @%s", getDescription(), typeName));
        }
        return annotation.get();
    }

    /**
     * @return The {@link Annotation} of the given type on the {@link #getPackageInfo() package-info.class}
     *         of this package or {@link Optional#empty()} if either there is no {@code package-info}
     *         or the {@code package-info} is not annotated with the respective annotation type.
     * @param <A> The type of the {@link Annotation} to retrieve
     * @see #getAnnotationOfType(Class)
     * @see #tryGetAnnotationOfType(String)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public <A extends Annotation> Optional<A> tryGetAnnotationOfType(Class<A> type) {
        if (packageInfo.isPresent()) {
            return packageInfo.get().tryGetAnnotationOfType(type);
        }
        return Optional.empty();
    }

    /**
     * @return The {@link JavaAnnotation} matching the given type on the {@link #getPackageInfo() package-info.class}
     *         of this package or {@link Optional#empty()} if either there is no {@code package-info}
     *         or the {@code package-info} is not annotated with the respective annotation type.
     * @see #tryGetAnnotationOfType(Class)
     * @see #getAnnotationOfType(String)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Optional<JavaAnnotation<JavaPackage>> tryGetAnnotationOfType(String typeName) {
        return packageInfo.flatMap(it -> it.tryGetAnnotationOfType(typeName).map(withSelfAsOwner));
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is annotated with an {@link Annotation} of the given type.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationType) {
        return packageInfo.map(it -> it.isAnnotatedWith(annotationType)).orElse(false);
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is annotated with an {@link Annotation} of the given type.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(String annotationTypeName) {
        return packageInfo.map(it -> it.isAnnotatedWith(annotationTypeName)).orElse(false);
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is annotated with an {@link Annotation} matching the given predicate.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return packageInfo.map(it -> it.isAnnotatedWith(predicate)).orElse(false);
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is meta-annotated with an {@link Annotation} of the given type.
     *         A meta-annotation is an annotation that is declared on another annotation.
     *         <p>
     *         This method also returns {@code true} if this element is directly annotated with the given annotation type.
     *         </p>
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return packageInfo.map(it -> it.isMetaAnnotatedWith(annotationType)).orElse(false);
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is meta-annotated with an {@link Annotation} of the given type.
     *         A meta-annotation is an annotation that is declared on another annotation.
     *         <p>
     *         This method also returns {@code true} if this element is directly annotated with the given annotation type.
     *         </p>
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(String annotationTypeName) {
        return packageInfo.map(it -> it.isMetaAnnotatedWith(annotationTypeName)).orElse(false);
    }

    /**
     * @return {@code true} if and only if there is a {@link #getPackageInfo() package-info.class} in this package
     *         that is annotated with an {@link Annotation} matching the given predicate.
     *         A meta-annotation is an annotation that is declared on another annotation.
     *         <p>
     *         This method also returns {@code true} if this element is directly annotated with the given annotation type.
     *         </p>
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return packageInfo.map(it -> it.isMetaAnnotatedWith(predicate)).orElse(false);
    }

    /**
     * @return the parent package, e.g. {@code java} for package {@code java.lang}
     */
    @PublicAPI(usage = ACCESS)
    public Optional<JavaPackage> getParent() {
        return parent;
    }

    private void setParent(JavaPackage parent) {
        this.parent = Optional.of(parent);
    }

    /**
     * @return all classes directly contained in this package, but not classes in the lower levels of the package tree (compare {@link #getClassesInPackageTree()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getClasses() {
        return classes;
    }

    /**
     * @return all classes contained in this {@link JavaPackage package tree}, i.e. all classes in this package,
     *         subpackages, subpackages of subpackages, and so on (compare {@link #getClasses()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getClassesInPackageTree() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.<JavaClass>builder().addAll(classes);
        for (JavaPackage subpackage : getSubpackages()) {
            result.addAll(subpackage.getClassesInPackageTree());
        }
        return result.build();
    }

    /**
     * @return all (direct) subpackages contained in this package, e.g. for package {@code java} this would be
     *         {@code [java.lang, java.io, ...]} (compare {@link #getSubpackagesInTree()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getSubpackages() {
        return ImmutableSet.copyOf(subpackages.values());
    }

    /**
     * @return all subpackages contained in the package tree of this package. I.e. all subpackages, subpackages
     *         of subpackages, and so on. For package {@code java} this would be
     *         {@code [java.lang, java.lang.annotation, java.util, java.util.concurrent, ...]} (compare {@link #getSubpackages()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getSubpackagesInTree() {
        ImmutableSet.Builder<JavaPackage> result = ImmutableSet.builder();
        for (JavaPackage subpackage : getSubpackages()) {
            result.add(subpackage);
            result.addAll(subpackage.getSubpackagesInTree());
        }
        return result.build();
    }

    /**
     * @param clazz a {@link JavaClass}
     * @return {@code true} if this package (directly) contains this {@link JavaClass}
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClass(JavaClass clazz) {
        return classes.contains(clazz);
    }

    /**
     * @param clazz a Java {@link Class}
     * @return {@code true} if this package (directly) contains a {@link JavaClass} equivalent to the supplied {@link Class}
     * @see #getClass(Class)
     * @see #containsClassWithFullyQualifiedName(String)
     * @see #containsClassWithSimpleName(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClass(Class<?> clazz) {
        return containsClassWithFullyQualifiedName(clazz.getName());
    }

    /**
     * @param clazz A Java {@link Class}
     * @return the class if (directly) contained in this package, otherwise an Exception is thrown
     * @see #containsClass(Class)
     * @see #getClassWithFullyQualifiedName(String)
     * @see #getClassWithSimpleName(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getClass(Class<?> clazz) {
        return getClassWithFullyQualifiedName(clazz.getName());
    }

    /**
     * @param className fully qualified name of a Java class
     * @return {@code true} if this package (directly) contains a {@link JavaClass} with the given fully qualified name
     * @see #getClassWithFullyQualifiedName(String)
     * @see #containsClass(Class)
     * @see #containsClassWithSimpleName(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClassWithFullyQualifiedName(String className) {
        return tryGetClassWithFullyQualifiedName(className).isPresent();
    }

    /**
     * @param className fully qualified name of a Java class
     * @return the class if (directly) contained in this package, otherwise an Exception is thrown
     * @see #containsClassWithFullyQualifiedName(String)
     * @see #getClass(Class)
     * @see #getClassWithSimpleName(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getClassWithFullyQualifiedName(String className) {
        return getValue(tryGetClassWithFullyQualifiedName(className),
                "This package does not contain any class with fully qualified name '%s'", className);
    }

    private Optional<JavaClass> tryGetClassWithFullyQualifiedName(String className) {
        return tryGetClassWith(GET_NAME.is(equalTo(className)));
    }

    /**
     * @param className simple name of a Java class
     * @return {@code true} if this package (directly) contains a {@link JavaClass} with the given simple name
     * @see #getClassWithSimpleName(String)
     * @see #containsClass(Class)
     * @see #containsClassWithFullyQualifiedName(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClassWithSimpleName(String className) {
        return tryGetClassWithSimpleName(className).isPresent();
    }

    /**
     * @param className simple name of a Java class
     * @return the class if (directly) contained in this package, otherwise an Exception is thrown
     * @see #containsClassWithSimpleName(String)
     * @see #getClass(Class)
     * @see #getClassWithFullyQualifiedName(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass getClassWithSimpleName(String className) {
        return getValue(tryGetClassWithSimpleName(className),
                "This package does not contain any class with simple name '%s'", className);
    }

    private Optional<JavaClass> tryGetClassWithSimpleName(String className) {
        return tryGetClassWith(GET_SIMPLE_NAME.is(equalTo(className)));
    }

    private Optional<JavaClass> tryGetClassWith(DescribedPredicate<? super JavaClass> predicate) {
        Set<JavaClass> matching = getClassesWith(predicate);
        return matching.size() == 1 ? Optional.of(getOnlyElement(matching)) : Optional.empty();
    }

    private Set<JavaClass> getClassesWith(Predicate<? super JavaClass> predicate) {
        return classes.stream().filter(predicate).collect(toSet());
    }

    /**
     * @param packageName (relative) name of a package, may consist of several parts, e.g. {@code some.subpackage}
     * @return true if this package contains the supplied (sub-)package with the given (relative) name
     * @see #getPackage(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsPackage(String packageName) {
        return tryGetPackage(packageName).isPresent();
    }

    /**
     * @param packageName (relative) name of a package, may consist of several parts, e.g. {@code some.subpackage}
     * @return the (sub-)package with the given (relative) name; throws an exception if there is no such package contained
     * @see #containsPackage(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaPackage getPackage(String packageName) {
        return getValue(tryGetPackage(packageName), "This package does not contain any sub package '%s'", packageName);
    }

    private Optional<JavaPackage> tryGetPackage(String packageName) {
        Deque<String> packageParts = new LinkedList<>(Splitter.on('.').splitToList(packageName));
        return tryGetPackage(this, packageParts);
    }

    private Optional<JavaPackage> tryGetPackage(JavaPackage currentPackage, Deque<String> packageParts) {
        if (packageParts.isEmpty()) {
            return Optional.of(currentPackage);
        }

        String next = packageParts.poll();
        JavaPackage child = subpackages.get(next);
        return child != null ? child.tryGetPackage(child, packageParts) : Optional.empty();
    }

    private <T> T getValue(Optional<T> optional, String errorMessageTemplate, Object... messageParams) {
        checkArgument(optional.isPresent(), errorMessageTemplate, messageParams);
        return optional.get();
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} (directly) within this package
     *         to a {@link JavaClass} outside of this package. For dependencies from the package tree
     *         (this package, subpackages, subpackages of subpackages, etc.) please refer to {@link #getClassDependenciesFromThisPackageTree()}.
     * @see #getClassDependenciesToThisPackage()
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesFromThisPackage() {
        return getClassDependenciesFrom(getClasses());
    }

    private static Set<Dependency> getClassDependenciesFrom(Set<JavaClass> classes) {
        return classes.stream()
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> !classes.contains(dependency.getTargetClass()))
                .collect(toImmutableSet());
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} within this package tree
     *         (this package, subpackages, subpackages of subpackages, etc.) to a {@link JavaClass} outside of this package tree.
     *         To limit this to dependencies that originate (directly) from this package please refer to {@link #getClassDependenciesFromThisPackage()}.
     * @see #getClassDependenciesToThisPackageTree()
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesFromThisPackageTree() {
        return getClassDependenciesFrom(getClassesInPackageTree());
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} outside of this package
     *         to a {@link JavaClass} (directly) within this package. For dependencies to this package tree
     *         (this package, subpackages, subpackages of subpackages, etc.) please refer to {@link #getClassDependenciesToThisPackageTree()}.
     * @see #getClassDependenciesFromThisPackage()
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesToThisPackage() {
        return getClassDependenciesTo(getClasses());
    }

    private static ImmutableSet<Dependency> getClassDependenciesTo(Set<JavaClass> classes) {
        return classes.stream()
                .flatMap(javaClass -> javaClass.getDirectDependenciesToSelf().stream())
                .filter(dependency -> !classes.contains(dependency.getOriginClass()))
                .collect(toImmutableSet());
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} outside of this package tree
     *         (this package, subpackages, subpackages of subpackages, etc.) to a {@link JavaClass} within this package tree.
     *         To limit this to dependencies that directly target this package refer to {@link #getClassDependenciesToThisPackage()}.
     * @see #getClassDependenciesFromThisPackageTree()
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesToThisPackageTree() {
        return getClassDependenciesTo(getClassesInPackageTree());
    }

    /**
     * @return All {@link JavaPackage packages} that this package has a dependency on. I.e. all {@link JavaPackage packages}
     *         that contain a class such that a class (directly) in this package depends on that class.
     *         For example <br><br>
     *         <img src="http://www.plantuml.com/plantuml/png/ZP11ImCn48Nl-HKFF1VlQoaz5C6B20gUqyrhDtGsMP9Pi4Z_kt7Zskr1y3QPz-QRbzbcjKd7Nam--J3OP7k83zJpGCIaNJHciEt97edNsFFbf_uqG2isP-nyOgjoVWhPEvhId_tFp4etMMTpDLMXj2_cOa9KGkE0U5XkQD2pR7TehGWTOe1sFl_2hx4UVSfw3LJVRpOTgJDUUD32hrA3zpYAmvveOr3h0nryG68UHHVVPP7T0bmRj-JaZzs4hkBCNkytHLxVxpT_hhcz6rQhpJ8vxgGCD8dkGVcXtmKQx3Wmk8mOpFAIqt0DyCzfiotLsCiN">
     *         <br><br>
     *         For dependencies to all packages that any class in this package tree (this package, subpackages, subpackages of subpackages, etc.)
     *         depends on refer to {@link #getPackageDependenciesFromThisPackageTree()}.
     *
     * @see #getClassDependenciesFromThisPackage()
     * @see #getPackageDependenciesToThisPackage()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesFromThisPackage() {
        return getPackageDependencies(getClassDependenciesFromThisPackage(), Dependency::getTargetClass);
    }

    /**
     * @return All {@link JavaPackage packages} that this package tree (this package, subpackages, subpackages of subpackages, etc.)
     *         has a dependency on. I.e. all {@link JavaPackage packages} that contain a class such that a class in this package tree
     *         depends on that class. For example
     *         <br><br>
     *         <img src="http://www.plantuml.com/plantuml/png/XP31QiCm44Jl-eg1dlC3BfGSGg6NKcYXPzMQ92BhbP7Mq53oxtKjt2YdtCQUsRUZtHRpsQP1N3b57Nts0oGgxJmIATinEJVw_kGFn7iQ-5OrVXpGYoy5kvZPcvnVjCH0vu0r_yfY34jq3TTGDHnmSHUdoGXB8zA-tT1XuBmzeFSY34WAEyRo3x_MUewvsBLG_Vxm-K1RySAzpVngTVXCamHy4NrIyr4P41MPPH9hdilP3Wsu_hWbvtWuBkXgtcFV7WkRpLDR5myIbLrcU3H-svz0Xnr7QYX8wjhpBSDjfRlJRZkhQP1V">
     *         <br><br>
     *         To limit this to only those packages that classes (directly) in this package depend on
     *         refer to {@link #getPackageDependenciesFromThisPackage()}.
     *
     * @see #getClassDependenciesFromThisPackageTree()
     * @see #getPackageDependenciesToThisPackageTree()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesFromThisPackageTree() {
        return getPackageDependencies(getClassDependenciesFromThisPackageTree(), Dependency::getTargetClass);
    }

    /**
     * @return All {@link JavaPackage packages} that have a dependency on this package.
     *         I.e. all {@link JavaPackage packages} that contain a class that depends on a class (directly) in this package.
     *         For example <br><br>
     *         <img src="http://www.plantuml.com/plantuml/png/XP4nQyCm48Nt-nKFEeVkhWaPGg5BM-ZGMROkiOXyCj8P6ah-U-K84sKesKv-xxvxljibDL7dqLCwEPbCD2Ey4zRpn0XHXq0qcVvaZeolCF9dgV5BGEzDIjYxoBtwyviaPva8MFtld9JjrRgQYwa815peGXAKeM52EDGJ6eoSfrlqndks208TN5hXjrIlBYQzvIdlZ-YEJbQwvuDcV94QV8VPMtICFeWQ9spX197JxBNwMywohS3bmpqvFuOhkeWhk-ssMMwVk-s_O-xNXdQpgPFZQJb24zc-AF_eKg31dYSMcn24waKkpNMwnzsqozLoz3y0">
     *         <br><br>
     *         For dependencies from all packages that depend on any class in this package tree (this package, subpackages, subpackages of subpackages, etc.)
     *         refer to {@link  #getPackageDependenciesToThisPackageTree()}.
     *
     * @see #getClassDependenciesToThisPackage()
     * @see #getPackageDependenciesFromThisPackage()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesToThisPackage() {
        return getPackageDependencies(getClassDependenciesToThisPackage(), Dependency::getOriginClass);
    }

    /**
     * @return All {@link JavaPackage packages} that have a dependency on this package tree (this package, subpackages, subpackages of subpackages, etc.).
     *         I.e. all {@link JavaPackage packages} that contain a class that depends on a class in this package tree. For example <br><br>
     *         <img src="http://www.plantuml.com/plantuml/png/XL4nRiCm3DpvYWCwvmDkXmn1WIvjWGwTLOc9XSYI8D90Wo9_hnp5Y39b8tN7u_6q5JL5vocG77tCffW9mKVMKsQaecCYoiOUpO7nbIR-lDP_1DXWHB3pXQs6qriKxvW8MFdlBChkGbt9ZTG00ivqffYKeIaZxViD0oQksnsi2O4TKeIENbRncwjNNqHlVbh_KVp1nrKzy5whV8C6VASvEFmmR8fgwh4EFgAmp46xQxd2hXDk3_VAjHalqWryf7sV5LusczukdfRaXyBkMVbNbm83TfmvEga1K9_UCjnRUZnDsrejF_qF">
     *         <br><br>
     *         To limit this to only those packages
     *         that depend on classes (directly) in this package refer to {@link #getPackageDependenciesToThisPackage()}.
     *
     * @see #getClassDependenciesToThisPackageTree()
     * @see #getPackageDependenciesFromThisPackageTree()
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesToThisPackageTree() {
        return getPackageDependencies(getClassDependenciesToThisPackageTree(), Dependency::getOriginClass);
    }

    private Set<JavaPackage> getPackageDependencies(Set<Dependency> dependencies, Function<Dependency, JavaClass> javaClassFromDependency) {
        return dependencies.stream()
                .map(javaClassFromDependency)
                .map(JavaClass::getPackage)
                .collect(toImmutableSet());
    }

    /**
     * Traverses the package tree visiting each matching class.
     * @param predicate determines which classes within the package tree should be visited
     * @param visitor will receive each class in the package tree matching the given predicate
     * @see #traversePackageTree(Predicate, PackageVisitor)
     */
    @PublicAPI(usage = ACCESS)
    public void traversePackageTree(Predicate<? super JavaClass> predicate, ClassVisitor visitor) {
        for (JavaClass javaClass : getClassesWith(predicate)) {
            visitor.visit(javaClass);
        }
        for (JavaPackage subpackage : getSubpackages()) {
            subpackage.traversePackageTree(predicate, visitor);
        }
    }

    /**
     * Traverses the package tree visiting each matching package.
     * @param predicate determines which packages within the package tree should be visited
     * @param visitor will receive each package in the package tree matching the given predicate
     * @see #traversePackageTree(Predicate, ClassVisitor)
     */
    @PublicAPI(usage = ACCESS)
    public void traversePackageTree(Predicate<? super JavaPackage> predicate, PackageVisitor visitor) {
        if (predicate.test(this)) {
            visitor.visit(this);
        }
        for (JavaPackage subpackage : getSubpackages()) {
            subpackage.traversePackageTree(predicate, visitor);
        }
    }

    @Override
    public String getDescription() {
        return "Package <" + name + ">";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }

    private final Function<? super JavaAnnotation<JavaClass>, JavaAnnotation<JavaPackage>> withSelfAsOwner = input -> input.withOwner(JavaPackage.this);

    static JavaPackage simple(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        JavaPackage defaultPackage = JavaPackage.from(singleton(javaClass));
        return packageName.isEmpty() ? defaultPackage : defaultPackage.getPackage(packageName);
    }

    static JavaPackage from(Iterable<JavaClass> classes) {
        return new Tree(classes).toJavaPackage();
    }

    private static class Tree {
        private final String packageName;
        private final Map<String, Tree> subpackageTrees;
        private final Set<JavaClass> classes = new HashSet<>();

        Tree(Iterable<JavaClass> classes) {
            this("", classes);
        }

        private Tree(String packageName, Iterable<JavaClass> classes) {
            this.packageName = packageName;

            SetMultimap<String, JavaClass> childPackages = HashMultimap.create();
            for (JavaClass clazz : classes) {
                if (clazz.getPackageName().equals(packageName)) {
                    this.classes.add(clazz);
                } else {
                    String subpackageName = findSubpackageName(packageName, clazz);
                    childPackages.put(subpackageName, clazz);
                }
            }
            this.subpackageTrees = createSubTrees(packageName, childPackages);
        }

        private String findSubpackageName(String packageName, JavaClass clazz) {
            String packageRest = !packageName.isEmpty()
                    ? clazz.getPackageName().substring(packageName.length() + 1)
                    : clazz.getPackageName();
            int indexOfDot = packageRest.indexOf(".");
            return indexOfDot > 0 ? packageRest.substring(0, indexOfDot) : packageRest;
        }

        private Map<String, Tree> createSubTrees(String packageName, SetMultimap<String, JavaClass> childPackages) {
            Map<String, Tree> result = new HashMap<>();
            for (Map.Entry<String, Collection<JavaClass>> entry : childPackages.asMap().entrySet()) {
                String childPackageName = joinSkippingEmpty(packageName, entry.getKey());
                result.put(entry.getKey(), new Tree(childPackageName, entry.getValue()));
            }
            return result;
        }

        private String joinSkippingEmpty(String first, String second) {
            return !first.isEmpty() ? first + "." + second : second;
        }

        JavaPackage toJavaPackage() {
            JavaPackage result = createJavaPackage();
            for (JavaPackage subpackage : result.getSubpackages()) {
                subpackage.setParent(result);
            }
            return result;
        }

        private JavaPackage createJavaPackage() {
            ImmutableMap.Builder<String, JavaPackage> subpackages = ImmutableMap.builder();
            for (Map.Entry<String, Tree> entry : subpackageTrees.entrySet()) {
                subpackages.put(entry.getKey(), entry.getValue().toJavaPackage());
            }
            return new JavaPackage(packageName, classes, subpackages.build());
        }
    }

    @PublicAPI(usage = INHERITANCE)
    public interface ClassVisitor {
        void visit(JavaClass javaClass);
    }

    @PublicAPI(usage = INHERITANCE)
    public interface PackageVisitor {
        void visit(JavaPackage javaPackage);
    }

    /**
     * Predefined {@link ChainableFunction functions} to transform {@link JavaPackage}.
     */
    @PublicAPI(usage = ACCESS)
    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaPackage, String> GET_RELATIVE_NAME =
                new ChainableFunction<JavaPackage, String>() {
                    @Override
                    public String apply(JavaPackage javaPackage) {
                        return javaPackage.getRelativeName();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaPackage, Set<JavaClass>> GET_CLASSES =
                new ChainableFunction<JavaPackage, Set<JavaClass>>() {
                    @Override
                    public Set<JavaClass> apply(JavaPackage javaPackage) {
                        return javaPackage.getClasses();
                    }
                };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaPackage, Set<JavaPackage>> GET_SUB_PACKAGES =
                new ChainableFunction<JavaPackage, Set<JavaPackage>>() {
                    @Override
                    public Set<JavaPackage> apply(JavaPackage javaPackage) {
                        return javaPackage.getSubpackages();
                    }
                };
    }
}
