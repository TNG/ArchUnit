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
package com.tngtech.archunit.junit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.reporting.ReportEntry;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

class ArchUnitExtensionContext implements ExtensionContext {

    private final AbstractArchUnitTestDescriptor descriptor;
    private final ArchUnitEngineExecutionContext context;

    public ArchUnitExtensionContext(
            AbstractArchUnitTestDescriptor descriptor,
            ArchUnitEngineExecutionContext execution) {
        this.descriptor = descriptor;
        this.context = execution;
    }

    @Override
    public Optional<ExtensionContext> getParent() {
        return descriptor.getParent()
                .map(AbstractArchUnitTestDescriptor.class::cast)
                .map(descriptor -> new ArchUnitExtensionContext(descriptor, context));
    }

    @Override
    public ExtensionContext getRoot() {
        ExtensionContext result = this;
        Optional<ExtensionContext> parent = getParent();
        while (parent.isPresent()) {
            result = parent.get();
            parent = parent.flatMap(ExtensionContext::getParent);
        }
        return result;
    }

    @Override
    public String getUniqueId() {
        return descriptor.getUniqueId().toString();
    }

    @Override
    public String getDisplayName() {
        return descriptor.getDisplayName();
    }

    @Override
    public Set<String> getTags() {
        return descriptor.getTags().stream()
                .map(TestTag::getName)
                .collect(toSet());
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return Optional.of(descriptor.getAnnotatedElement());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Class<?>> getTestClass() {
        return descriptor.getAnnotatedElement().findChild(Class.class)
                .map(Class.class::cast);
    }

    @Override
    public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
        return Optional.of(PER_METHOD);
    }

    @Override
    public Optional<Object> getTestInstance() {
        return Optional.empty();
    }

    @Override
    public Optional<TestInstances> getTestInstances() {
        return Optional.empty();
    }

    @Override
    public Optional<Method> getTestMethod() {
        return descriptor.getAnnotatedElement().findChild(Method.class);
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getConfigurationParameter(String key) {
        return context.getConfigurationParameters().get(key);
    }

    @Override
    public <T> Optional<T> getConfigurationParameter(String key, Function<String, T> transformer) {
        return context.getConfigurationParameters().get(key, transformer);
    }

    @Override
    public void publishReportEntry(Map<String, String> map) {
        context.getEngineExecutionListener()
                .reportingEntryPublished(descriptor, ReportEntry.from(map));
    }

    @Override
    public Store getStore(Namespace namespace) {
        return new NamespacedStore(namespace, descriptor.getStore(), getParentStore().orElse(null));
    }

    @Override
    public org.junit.jupiter.api.parallel.ExecutionMode getExecutionMode() {
        return org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
    }

    private Optional<Map<NamespacedStore.NamespacedKey, Object>> getParentStore() {
        return descriptor.getParent()
                .map(AbstractArchUnitTestDescriptor.class::cast)
                .map(AbstractArchUnitTestDescriptor::getStore);
    }
}
