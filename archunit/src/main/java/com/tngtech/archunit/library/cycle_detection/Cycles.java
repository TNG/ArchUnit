/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.Collection;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface Cycles<EDGE extends Edge<?>> extends Collection<Cycle<EDGE>> {

    /**
     * @return {@code true}, if the maximum number of cycles to detect had been reached.
     *         I.e. if {@code true} there could be more cycles in the examined graph that are omitted from the result,
     *         if {@code false} then all the cycles of the graph are reported.<br><br>
     *         The maximum number of cycles at which the algorithm will stop can be configured by the {@code archunit.properties}
     *         property {@value CycleConfiguration#MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME}.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    boolean maxNumberOfCyclesReached();
}
