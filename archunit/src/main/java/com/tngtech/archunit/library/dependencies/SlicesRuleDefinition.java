/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.library.dependencies.syntax.SlicesShould;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Allows to specify {@link ArchRule ArchRules} for "slices" of a code base. A slice is conceptually
 * a cut through a code base according to business logic. Take for example
 * <pre><code>
 * com.mycompany.myapp.order
 * com.mycompany.myapp.customer
 * com.mycompany.myapp.user
 * com.mycompany.myapp.authorization
 * </code></pre>
 * The top level packages under 'myapp' are composed according to different domain aspects. It is
 * good practice, to keep such packages free of cycles, which is one capability that this class
 * provides.<br>
 * Consider
 * <pre><code>
 * {@link #slices() slices()}.{@link Slices#matching(String) matching("..myapp.(*)..")}.{@link GivenSlices#should() should()}.{@link SlicesShould#beFreeOfCycles() beFreeOfCycles()}
 * </code></pre>
 * Then this rule will assert, that the four slices of 'myapp' are free of cycles.
 */
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

    public static class Creator {
        private Creator() {
        }

        /**
         * @see Slices#matching(String)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices matching(String packageIdentifier) {
            return new GivenSlicesInternal(Priority.MEDIUM, Slices.matching(packageIdentifier));
        }

        /**
         * @see Slices#assignedFrom(SliceAssignment)
         */
        @PublicAPI(usage = ACCESS)
        public GivenSlices assignedFrom(SliceAssignment assignment) {
            return new GivenSlicesInternal(Priority.MEDIUM, Slices.assignedFrom(assignment));
        }
    }
}
