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
package com.tngtech.archunit.junit.internal.filtering;

import com.tngtech.archunit.junit.internal.ArchUnitEngineDescriptor;
import com.tngtech.archunit.junit.internal.ArchUnitTestEngine;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface TestSourceFilter {
    Logger LOG = LoggerFactory.getLogger(ArchUnitTestEngine.class);

    TestSourceFilter NOOP = new TestSourceFilter() {
        @Override
        public boolean shouldRun(TestSource source) {
            return true;
        }

        @Override
        public String toString() {
            return "NOOP";
        }
    };

    default boolean shouldRun(TestDescriptor descriptor) {
        return descriptor.getSource()
                .map(this::shouldRun)
                .orElse(true);
    }

    boolean shouldRun(TestSource source);

    static TestSourceFilter forRequest(EngineDiscoveryRequest discoveryRequest, ArchUnitEngineDescriptor engineDescriptor) {
        try {
            if (ArchUnitPropsTestSourceFilter.appliesTo(discoveryRequest, engineDescriptor)) {
                return new ArchUnitPropsTestSourceFilter(discoveryRequest, engineDescriptor);
            }
        } catch (Exception e) {
            LOG.warn("Received error trying to apply test name filter from testing tool", e);
        }

        return TestSourceFilter.NOOP;
    }
}
