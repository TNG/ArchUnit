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
package com.tngtech.archunit.junit;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import static com.tngtech.archunit.junit.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.ReflectionUtils.withAnnotation;
import static java.util.stream.Collectors.toList;

/**
 * A simple test engine to discover and execute ArchUnit tests with JUnit 5. In particular the engine
 * uses a {@link ClassCache} to avoid the costly import process as much as possible.
 * <br><br>
 * Mark classes to be executed by the {@link ArchUnitTestEngine} with {@link AnalyzeClasses @AnalyzeClasses} and
 * rule fields or methods with {@link ArchTest @ArchTest}. Example:
 * <pre><code>
 *{@literal @}AnalyzeClasses(packages = "com.foo")
 * class MyArchTest {
 *    {@literal @}ArchTest
 *     public static final ArchRule myRule = classes()...
 * }
 * </code></pre>
 */
@Internal
public final class ArchUnitTestEngine extends HierarchicalTestEngine<ArchUnitEngineExecutionContext> {
    static final String UNIQUE_ID = "archunit";

    private SharedCache cache = new SharedCache(); // NOTE: We want to change this in tests -> no static/final reference

    @Override
    public String getId() {
        return UNIQUE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ArchUnitEngineDescriptor result = new ArchUnitEngineDescriptor(uniqueId);

        resolveRequestedClasspathRoot(discoveryRequest, uniqueId, result);
        resolveRequestedPackages(discoveryRequest, uniqueId, result);
        resolveRequestedClasses(discoveryRequest, uniqueId, result);
        resolveRequestedMethods(discoveryRequest, uniqueId, result);
        resolveRequestedFields(discoveryRequest, uniqueId, result);
        resolveRequestedUniqueIds(discoveryRequest, uniqueId, result);

        return result;
    }

    private void resolveRequestedClasspathRoot(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        Stream<JavaClass> classes = discoveryRequest.getSelectorsByType(ClasspathRootSelector.class).stream()
                .flatMap(this::getContainedClasses);
        filterCandidatesAndLoadClasses(classes, discoveryRequest)
                .forEach(clazz -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, clazz), cache.get()));
    }

    private void resolveRequestedPackages(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        String[] packages = discoveryRequest.getSelectorsByType(PackageSelector.class).stream()
                .map(PackageSelector::getPackageName)
                .toArray(String[]::new);
        Stream<JavaClass> classes = getContainedClasses(packages);

        filterCandidatesAndLoadClasses(classes, discoveryRequest)
                .forEach(clazz -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, clazz), cache.get()));
    }

    private Stream<Class<?>> filterCandidatesAndLoadClasses(Stream<JavaClass> classes, EngineDiscoveryRequest discoveryRequest) {
        return classes
                .filter(isAllowedBy(discoveryRequest))
                .filter(this::isArchUnitTestCandidate)
                .flatMap(this::safelyReflect);
    }

    private void resolveRequestedClasses(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        discoveryRequest.getSelectorsByType(ClassSelector.class).stream()
                .map(ClassSelector::getJavaClass)
                .filter(this::isArchUnitTestCandidate)
                .forEach(clazz -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, clazz), cache.get()));
    }

    private void resolveRequestedMethods(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        discoveryRequest.getSelectorsByType(MethodSelector.class).stream()
                .filter(s -> s.getJavaMethod().isAnnotationPresent(ArchTest.class))
                .forEach(selector -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, selector.getJavaClass(), selector.getJavaMethod()), cache.get()));
    }

    private void resolveRequestedFields(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        discoveryRequest.getSelectorsByType(FieldSelector.class).stream()
                .filter(s -> s.getJavaField().isAnnotationPresent(ArchTest.class))
                .forEach(selector -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, selector.getJavaClass(), selector.getJavaField()), cache.get()));
    }

    private void resolveRequestedUniqueIds(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, ArchUnitEngineDescriptor result) {
        discoveryRequest.getSelectorsByType(UniqueIdSelector.class).stream()
                .filter(selector -> selector.getUniqueId().getEngineId().equals(Optional.of(getId())))
                .forEach(selector -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, selector.getUniqueId()), cache.get()));
    }

    private Stream<JavaClass> getContainedClasses(String[] packages) {
        return stream(new ClassFileImporter().importPackages(packages));
    }

    private Stream<JavaClass> getContainedClasses(ClasspathRootSelector selector) {
        return stream(new ClassFileImporter().importUrl(toUrl(selector.getClasspathRoot())));
    }

    private Stream<JavaClass> stream(JavaClasses classes) {
        return StreamSupport.stream(classes.spliterator(), false);
    }

    private Predicate<JavaClass> isAllowedBy(EngineDiscoveryRequest discoveryRequest) {
        List<Predicate<String>> filters = Stream
                .concat(discoveryRequest.getFiltersByType(ClassNameFilter.class).stream(),
                        discoveryRequest.getFiltersByType(PackageNameFilter.class).stream())
                .map(Filter::toPredicate)
                .collect(toList());

        return javaClass -> filters.stream().allMatch(p -> p.test(javaClass.getName()));
    }

    private boolean isArchUnitTestCandidate(JavaClass javaClass) {
        return javaClass.getAllMembers().stream().anyMatch(m -> m.isAnnotatedWith(ArchTest.class));
    }

    @MayResolveTypesViaReflection(reason = "Within the ArchUnitTestEngine we may resolve types via reflection, since they are needed anyway")
    private Stream<Class<?>> safelyReflect(JavaClass javaClass) {
        try {
            return Stream.of(javaClass.reflect());
        } catch (NoClassDefFoundError | RuntimeException e) {
            return Stream.empty();
        }
    }

    private boolean isArchUnitTestCandidate(Class<?> clazz) {
        try {
            return !getAllFields(clazz, withAnnotation(ArchTest.class)).isEmpty()
                    || !getAllMethods(clazz, withAnnotation(ArchTest.class)).isEmpty();
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    private URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new ArchTestInitializationException(e);
        }
    }

    @Override
    protected ArchUnitEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new ArchUnitEngineExecutionContext();
    }

    static class SharedCache {
        private static final ClassCache cache = new ClassCache();

        ClassCache get() {
            return cache;
        }
    }
}
