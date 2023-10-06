/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.cycle_detection;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.Internal;

@Internal
public final class CycleConfiguration {
    /**
     * Configures the maximum number of cycles to detect by {@link CycleDetector}. I.e. once this number
     * of cycles has been found the algorithm will stop its search and report the cycles found so far.
     */
    @Internal
    public static final String MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME = "cycles.maxNumberToDetect";
    private static final String MAX_NUMBER_OF_CYCLES_TO_DETECT_DEFAULT_VALUE = "100";

    private final int maxCyclesToDetect;

    CycleConfiguration() {
        String configuredMaxCyclesToDetect = ArchConfiguration.get()
                .getPropertyOrDefault(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME, MAX_NUMBER_OF_CYCLES_TO_DETECT_DEFAULT_VALUE);
        maxCyclesToDetect = Integer.parseInt(configuredMaxCyclesToDetect);
    }

    int getMaxNumberOfCyclesToDetect() {
        return maxCyclesToDetect;
    }
}
