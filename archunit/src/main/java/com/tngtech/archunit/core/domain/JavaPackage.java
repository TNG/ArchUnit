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

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_SIMPLE_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static java.util.Collections.singleton;

public final class JavaPackage implements HasName {
    private final String name;
    private final String relativeName;
    private final Set<JavaClass> classes;
    private final Map<String, JavaPackage> subPackages;
    private Optional<JavaPackage> parent = Optional.absent();

    private JavaPackage(String name, Set<JavaClass> classes, Map<String, JavaPackage> subPackages) {
        this.name = checkNotNull(name);
        relativeName = name.substring(name.lastIndexOf(".") + 1);
        this.classes = ImmutableSet.copyOf(classes);
        this.subPackages = ImmutableMap.copyOf(subPackages);
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
     * @return all classes directly contained in this package, no classes in sub-packages (compare {@link #getAllClasses()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getClasses() {
        return classes;
    }

    /**
     * @return all classes contained in this package or any sub-package (compare {@link #getClasses()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaClass> getAllClasses() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.<JavaClass>builder().addAll(classes);
        for (JavaPackage subPackage : getSubPackages()) {
            result.addAll(subPackage.getAllClasses());
        }
        return result.build();
    }

    /**
     * @return all (direct) sub-packages contained in this package, e.g. {@code [java.lang, java.io, ...]} for package {@code java}
     * (compare {@link #getAllSubPackages()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getSubPackages() {
        return ImmutableSet.copyOf(subPackages.values());
    }

    /**
     * @return all sub-packages including nested sub-packages contained in this package,
     * e.g. {@code [java.lang, java.lang.annotation, java.util, java.util.concurrent, ...]} for package {@code java}
     * (compare {@link #getSubPackages()})
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getAllSubPackages() {
        ImmutableSet.Builder<JavaPackage> result = ImmutableSet.builder();
        for (JavaPackage subPackage : getSubPackages()) {
            result.add(subPackage);
            result.addAll(subPackage.getAllSubPackages());
        }
        return result.build();
    }

    /**
     * @param clazz a {@link JavaClass}
     * @return true if this package (directly) contains this {@link JavaClass}
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClass(JavaClass clazz) {
        return classes.contains(clazz);
    }

    /**
     * @param clazz a Java {@link Class}
     * @return true if this package (directly) contains a {@link JavaClass} equivalent to the supplied {@link Class}
     * @see #getClass(Class)
     * @see #containsClassWithFullyQualifiedName(String)
     * @see #containsClassWithSimpleName(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsClass(Class<?> clazz) {
        return containsClassWithFullyQualifiedName(clazz.getName());
    }

    /**
     * @param clazz A Java class
     * @return the class if contained in this package, otherwise an Exception is thrown
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
     * @return true if this package (directly) contains a {@link JavaClass} with the given fully qualified name
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
     * @return the class if contained in this package, otherwise an Exception is thrown
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
     * @return true if this package (directly) contains a {@link JavaClass} with the given simple name
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
     * @return the class if contained in this package, otherwise an Exception is thrown
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
        return matching.size() == 1 ? Optional.of(getOnlyElement(matching)) : Optional.<JavaClass>absent();
    }

    private Set<JavaClass> getClassesWith(Predicate<? super JavaClass> predicate) {
        Set<JavaClass> result = new HashSet<>();
        for (JavaClass javaClass : classes) {
            if (predicate.apply(javaClass)) {
                result.add(javaClass);
            }
        }
        return result;
    }

    /**
     * @param packageName name of a package, may consist of several parts, e.g. {@code some.subpackage}
     * @return true if this package contains the supplied (sub-) package
     * @see #getPackage(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsPackage(String packageName) {
        return tryGetPackage(packageName).isPresent();
    }

    /**
     * @param packageName name of a package, may consist of several parts, e.g. {@code some.subpackage}
     * @return the (sub-) package with the given (relative) name; throws an exception if there is no such package contained
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
        if (!subPackages.containsKey(next)) {
            return Optional.absent();
        }
        JavaPackage child = subPackages.get(next);
        return child.tryGetPackage(child, packageParts);
    }

    private <T> T getValue(Optional<T> optional, String errorMessageTemplate, Object... messageParams) {
        checkArgument(optional.isPresent(), errorMessageTemplate, messageParams);
        return optional.get();
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} within this package
     * to a {@link JavaClass} outside of this package
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : getAllClasses()) {
            addAllNonSelfDependencies(result, javaClass.getDirectDependenciesFromSelf());
        }
        return result.build();
    }

    /**
     * @return All {@link Dependency dependencies} that originate from a {@link JavaClass} outside of this package
     * to a {@link JavaClass} within this package
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesToSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : getAllClasses()) {
            addAllNonSelfDependencies(result, javaClass.getDirectDependenciesToSelf());
        }
        return result.build();
    }

    private void addAllNonSelfDependencies(ImmutableSet.Builder<Dependency> result, Set<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (!containsClass(dependency.getOriginClass()) || !containsClass(dependency.getTargetClass())) {
                result.add(dependency);
            }
        }
    }

    /**
     * @return All {@link JavaPackage packages} that this package has a dependency on. I.e. all {@link JavaPackage packages}
     * that contain a class such that a class in this package depends on that class.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesFromSelf() {
        ImmutableSet.Builder<JavaPackage> result = ImmutableSet.builder();
        for (Dependency dependency : getClassDependenciesFromSelf()) {
            result.add(dependency.getTargetClass().getPackage());
        }
        return result.build();
    }

    /**
     * @return All {@link JavaPackage packages} that have a dependency on this package. I.e. all {@link JavaPackage packages}
     * that contain a class that depends on a class in this package.
     */
    @PublicAPI(usage = ACCESS)
    public Set<JavaPackage> getPackageDependenciesToSelf() {
        ImmutableSet.Builder<JavaPackage> result = ImmutableSet.builder();
        for (Dependency dependency : getClassDependenciesToSelf()) {
            result.add(dependency.getOriginClass().getPackage());
        }
        return result.build();
    }

    /**
     * Traverses the package tree visiting each matching class.
     * @param predicate determines which classes within the package tree should be visited
     * @param visitor will receive each class in the package tree matching the given predicate
     * @see #accept(Predicate, PackageVisitor)
     */
    @PublicAPI(usage = ACCESS)
    public void accept(Predicate<? super JavaClass> predicate, ClassVisitor visitor) {
        for (JavaClass javaClass : getClassesWith(predicate)) {
            visitor.visit(javaClass);
        }
        for (JavaPackage subPackage : getSubPackages()) {
            subPackage.accept(predicate, visitor);
        }
    }

    /**
     * Traverses the package tree visiting each matching package.
     * @param predicate determines which packages within the package tree should be visited
     * @param visitor will receive each package in the package tree matching the given predicate
     * @see #accept(Predicate, ClassVisitor)
     */
    @PublicAPI(usage = ACCESS)
    public void accept(Predicate<? super JavaPackage> predicate, PackageVisitor visitor) {
        if (predicate.apply(this)) {
            visitor.visit(this);
        }
        for (JavaPackage subPackage : getSubPackages()) {
            subPackage.accept(predicate, visitor);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }

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
        private final Map<String, Tree> subPackageTrees;
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
                    String subPackageName = findSubPackageName(packageName, clazz);
                    childPackages.put(subPackageName, clazz);
                }
            }
            this.subPackageTrees = createSubTrees(packageName, childPackages);
        }

        private String findSubPackageName(String packageName, JavaClass clazz) {
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
            for (JavaPackage subPackage : result.getSubPackages()) {
                subPackage.setParent(result);
            }
            return result;
        }

        private JavaPackage createJavaPackage() {
            ImmutableMap.Builder<String, JavaPackage> subPackages = ImmutableMap.builder();
            for (Map.Entry<String, Tree> entry : subPackageTrees.entrySet()) {
                subPackages.put(entry.getKey(), entry.getValue().toJavaPackage());
            }
            return new JavaPackage(packageName, classes, subPackages.build());
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
                        return javaPackage.getSubPackages();
                    }
                };
    }
}
