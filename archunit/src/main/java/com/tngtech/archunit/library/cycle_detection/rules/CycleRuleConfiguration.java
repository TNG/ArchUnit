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
package com.tngtech.archunit.library.cycle_detection.rules;

import com.tngtech.archunit.ArchConfiguration;

final class CycleRuleConfiguration {
    static final String MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME = "cycles.maxNumberOfDependenciesPerEdge";
    private static final String MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_DEFAULT_VALUE = "20";

    private final int maxDependenciesPerEdge;

    CycleRuleConfiguration() {
        String configuredMaxDependenciesPerEdge = ArchConfiguration.get()
                .getPropertyOrDefault(MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME,
                        MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_DEFAULT_VALUE);
        maxDependenciesPerEdge = Integer.parseInt(configuredMaxDependenciesPerEdge);
    }

    int getMaxNumberOfDependenciesToShowPerEdge() {
        return maxDependenciesPerEdge;
    }
}
