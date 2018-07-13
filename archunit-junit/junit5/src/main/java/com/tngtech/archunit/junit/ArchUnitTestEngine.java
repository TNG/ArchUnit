/*
 * Copyright 2018 TNG Technology Consulting GmbH
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

import java.util.Optional;

import com.tngtech.archunit.Internal;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

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
    static final String UNIQUE_ID = "junit-archunit";

    private SharedCache cache = new SharedCache(); // NOTE: We want to change this in tests -> no static/final reference

    @Override
    public String getId() {
        return UNIQUE_ID;
    }

    // FIXME: Throw exception if class is meant for test engine but @AnalyzeClasses is missing
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ArchUnitEngineDescriptor result = new ArchUnitEngineDescriptor(uniqueId);
        discoveryRequest.getSelectorsByType(UniqueIdSelector.class).stream()
                .filter(selector -> selector.getUniqueId().getEngineId().equals(Optional.of(getId())))
                .forEach(selector -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, selector.getUniqueId()), cache.get()));
        discoveryRequest.getSelectorsByType(ClassSelector.class)
                .forEach(selector -> ArchUnitTestDescriptor.resolve(
                        result, ElementResolver.create(result, uniqueId, selector.getJavaClass()), cache.get()));
        return result;
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
