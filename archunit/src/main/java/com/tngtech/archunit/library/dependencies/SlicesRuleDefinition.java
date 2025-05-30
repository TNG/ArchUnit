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
package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.library.dependencies.syntax.SlicesShould;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Allows to specify {@link ArchRule ArchRules} for "slices" of a code base.
 * A slice is conceptually a cut through a code base according to business logic.
 * <h6>Example</h6>
 * <pre><code>
 * com.mycompany.myapp.order
 * com.mycompany.myapp.customer
 * com.mycompany.myapp.user
 * com.mycompany.myapp.authorization
 * </code></pre>
 * The top level packages under {@code myapp} are composed according to different domain aspects.
 * It is good practice to keep such packages free of cycles,
 * which can be tested with the following rule:
 * <pre><code>{@link #slices() slices()}.{@link Creator#matching(String) matching("..myapp.(*)..")}.{@link GivenSlices#should() should()}.{@link SlicesShould#beFreeOfCycles() beFreeOfCycles()}</code></pre>
 * This rule asserts that the four slices of {@code myapp} are free of cycles.
 */
@PublicAPI(usage = ACCESS)
public final class SlicesRuleDefinition {
    private SlicesRuleDefinition() {
    }

    /**
     * Entry point into {@link SlicesRuleDefinition}
     */
    @PublicAPI(usage = ACCESS)
    public static Creator slices() {
        return new Creator();
    }

    @PublicAPI(usage = ACCESS)
    public static class Creator {
        private Creator() {
        }

        /**
         * defines a {@link SlicesRuleDefinition "slices" rule}
         * based on a {@link PackageMatcher package identifier} with capturing groups
         * @see Slices#matching(String)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices matching(String packageIdentifier) {
            return matching(packageIdentifier, Priority.MEDIUM);
        }

        /**
         * defines a {@link SlicesRuleDefinition "slices" rule}
         * based on a {@link PackageMatcher package identifier} with capturing groups
         * @see Slices#matching(String)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices matching(String packageIdentifier, Priority priority) {
            return new GivenSlicesInternal(priority, Slices.matching(packageIdentifier));
        }

        /**
         * defines a {@link SlicesRuleDefinition "slices" rule}
         * based on an explicit {@link SliceAssignment}
         * @see Slices#assignedFrom(SliceAssignment)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices assignedFrom(SliceAssignment assignment) {
            return assignedFrom(assignment, Priority.MEDIUM);
        }

        /**
         * defines a {@link SlicesRuleDefinition "slices" rule}
         * based on an explicit {@link SliceAssignment}
         * @see Slices#assignedFrom(SliceAssignment)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices assignedFrom(SliceAssignment assignment, Priority priority) {
            return new GivenSlicesInternal(priority, Slices.assignedFrom(assignment));
        }
    }
}
