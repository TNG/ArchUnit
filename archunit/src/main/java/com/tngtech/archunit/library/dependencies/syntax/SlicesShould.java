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
package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.library.dependencies.SliceRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface SlicesShould {
    /**
     * @return a {@link SliceRule} asserting that
     * there are no cycles via dependencies between the evaluated slices
     * (which is weaker than requiring
     * that the slices do {@link #notDependOnEachOther()} at all)
     */
    @PublicAPI(usage = ACCESS)
    SliceRule beFreeOfCycles();

    /**
     * @return a {@link SliceRule} asserting that
     * there are no dependencies at all between the evaluated slices
     * (which is stronger than requiring
     * that {@link #beFreeOfCycles() there are no dependency cycles} between the slices)
     */
    @PublicAPI(usage = ACCESS)
    SliceRule notDependOnEachOther();
}
