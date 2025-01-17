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
package com.tngtech.archunit.library.cycle_detection;

import java.util.List;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.Convertible;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.stream.Collectors.toSet;

/**
 * A cycle formed by the referenced {@code EDGEs}. A cycle in this context always refers to a "simple" cycle,
 * i.e. the list of edges is not empty, the {@link Edge#getOrigin() origin} of the first {@link Edge} is equal
 * to the {@link Edge#getTarget() target} of the last {@link Edge} and every node contained in the cycle
 * is contained exactly once.
 *
 * @param <EDGE> The type of the edges forming the cycle
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface Cycle<EDGE extends Edge<?>> extends Convertible {

    /**
     * @return The edges of the {@link Cycle}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    List<EDGE> getEdges();

    @Override
    default <T> Set<T> convertTo(Class<T> type) {
        return getEdges().stream()
                .filter(edge -> edge instanceof Convertible)
                .flatMap(edge -> ((Convertible) edge).convertTo(type).stream())
                .collect(toSet());
    }
}
