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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.ForwardingCollection;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext.AccessContext;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class JavaClasses extends ForwardingCollection<JavaClass> implements DescribedIterable<JavaClass>, CanOverrideDescription<JavaClasses> {
    private final ImmutableMap<String, JavaClass> classes;
    private final JavaPackage defaultPackage;
    private final String description;

    private JavaClasses(JavaPackage defaultPackage, Map<String, JavaClass> classes) {
        this(defaultPackage, classes, "classes");
    }

    private JavaClasses(JavaPackage defaultPackage, Map<String, JavaClass> classes, String description) {
        this.classes = ImmutableMap.copyOf(classes);
        this.defaultPackage = checkNotNull(defaultPackage);
        this.description = checkNotNull(description);
    }

    /**
     * @param predicate a {@link DescribedPredicate} to determine which classes match
     * @return {@link JavaClasses} matching the given predicate; the description will be adjusted according to the predicate's description
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses that(DescribedPredicate<? super JavaClass> predicate) {
        Map<String, JavaClass> matchingElements = Guava.Maps.filterValues(classes, predicate);
        String newDescription = String.format("%s that %s", description, predicate.getDescription());
        return new JavaClasses(defaultPackage, matchingElements, newDescription);
    }

    @Override
    public JavaClasses as(String description) {
        return new JavaClasses(defaultPackage, classes, description);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{classes=" + classes + '}';
    }

    /**
     * @param reflectedType a Java {@link Class} object
     * @return true, if an equivalent {@link JavaClass} is contained, false otherwise
     * @see #get(Class)
     * @see #contain(String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean contain(Class<?> reflectedType) {
        return contain(reflectedType.getName());
    }

    /**
     * @param reflectedType a Java {@link Class} object
     * @return a {@link JavaClass} equivalent to the given type; throws an exception if there is no equivalent class
     * @see #contain(Class)
     * @see #get(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass get(Class<?> reflectedType) {
        return get(reflectedType.getName());
    }

    /**
     * @param typeName a fully qualified name of a Java class
     * @return true, if a {@link JavaClass} with the given name is contained, false otherwise
     * @see #get(String)
     * @see #contain(Class)
     */
    @PublicAPI(usage = ACCESS)
    public boolean contain(String typeName) {
        return classes.containsKey(typeName);
    }

    /**
     * @param typeName a fully qualified name of a Java class
     * @return a {@link JavaClass} with the given name; throws an exception if there is no class with the given name
     * @see #contain(Class)
     * @see #get(Class)
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass get(String typeName) {
        checkArgument(contain(typeName), "%s do not contain %s of type %s",
                getClass().getSimpleName(), JavaClass.class.getSimpleName(), typeName);

        return classes.get(typeName);
    }

    /**
     * @param packageName name of a package, may consist of several parts, e.g. {@code com.myapp.some.subpackage}
     * @return true, if some package with this name is contained, false otherwise
     */
    @PublicAPI(usage = ACCESS)
    public boolean containPackage(String packageName) {
        return defaultPackage.containsPackage(packageName);
    }

    /**
     * @param packageName name of a package, may consist of several parts, e.g. {@code com.myapp.some.subpackage}
     * @return the package with the given name; throws an exception if the package does not exist
     * @see #containPackage(String)
     */
    @PublicAPI(usage = ACCESS)
    public JavaPackage getPackage(String packageName) {
        return defaultPackage.getPackage(packageName);
    }

    /**
     * @return the default package, i.e. the root of all packages. The default package name ist the empty string.
     */
    @PublicAPI(usage = ACCESS)
    public JavaPackage getDefaultPackage() {
        return defaultPackage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classes.keySet(), description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaClasses other = (JavaClasses) obj;
        return Objects.equals(this.classes.keySet(), other.classes.keySet())
                && Objects.equals(this.description, other.description);
    }

    @Override
    protected Collection<JavaClass> delegate() {
        return classes.values();
    }

    static JavaClasses of(Iterable<JavaClass> classes) {
        Map<String, JavaClass> mapping = new HashMap<>();
        for (JavaClass clazz : classes) {
            mapping.put(clazz.getName(), clazz);
        }
        JavaPackage defaultPackage = !Iterables.isEmpty(classes)
                ? getRoot(classes.iterator().next().getPackage())
                : JavaPackage.from(classes);
        return new JavaClasses(defaultPackage, mapping);
    }

    private static JavaPackage getRoot(JavaPackage javaPackage) {
        JavaPackage result = javaPackage;
        while (result.getParent().isPresent()) {
            result = result.getParent().get();
        }
        return result;
    }

    static JavaClasses of(
            Map<String, JavaClass> selectedClasses, Map<String, JavaClass> allClasses, ImportContext importContext) {

        CompletionProcess completionProcess = new CompletionProcess(allClasses.values(), importContext);
        JavaPackage defaultPackage = JavaPackage.from(allClasses.values());
        for (JavaClass clazz : allClasses.values()) {
            setPackage(clazz, defaultPackage);
            completionProcess.completeClass(clazz);
        }
        completionProcess.finish();
        return new JavaClasses(defaultPackage, selectedClasses);
    }

    private static void setPackage(JavaClass clazz, JavaPackage defaultPackage) {
        JavaPackage javaPackage = clazz.getPackageName().isEmpty()
                ? defaultPackage
                : defaultPackage.getPackage(clazz.getPackageName());
        clazz.setPackage(javaPackage);
    }

    private static class CompletionProcess {
        private final Set<JavaClass.CompletionProcess> classCompletionProcesses = new HashSet<>();
        private final Collection<JavaClass> classes;
        private final ImportContext context;

        CompletionProcess(Collection<JavaClass> classes, ImportContext context) {
            this.classes = classes;
            this.context = context;
        }

        void completeClass(JavaClass clazz) {
            classCompletionProcesses.add(clazz.completeFrom(context));
        }

        void finish() {
            AccessContext.TopProcess accessCompletionProcess = new AccessContext.TopProcess(classes);
            for (JavaClass.CompletionProcess process : classCompletionProcesses) {
                accessCompletionProcess.mergeWith(process.completeCodeUnitsFrom(context));
            }
            accessCompletionProcess.finish();
        }
    }
}
