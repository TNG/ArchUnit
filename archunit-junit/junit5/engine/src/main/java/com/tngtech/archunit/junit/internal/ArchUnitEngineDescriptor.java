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
package com.tngtech.archunit.junit.internal;

import com.tngtech.archunit.junit.internal.filtering.TestSourceFilter;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

class ArchUnitEngineDescriptor extends EngineDescriptor implements Node<ArchUnitEngineExecutionContext> {
    ArchUnitEngineDescriptor(UniqueId uniqueId) {
        super(uniqueId, "ArchUnit JUnit 5");
    }

    private TestSourceFilter additionalFilter = TestSourceFilter.NOOP;

    public void setAdditionalFilter(TestSourceFilter additionalFilter) {
        this.additionalFilter = additionalFilter;
    }

    public TestSourceFilter getAdditionalFilter() {
        return additionalFilter;
    }
}
