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
package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class SlicesRuleDefinition {
    private SlicesRuleDefinition() {
    }

    @PublicAPI(usage = ACCESS)
    public static Creator slices() {
        return new Creator();
    }

    public static class Creator {
        private Creator() {
        }

        @PublicAPI(usage = ACCESS)
        public GivenSlices matching(String packageIdentifier) {
            return new GivenSlicesInternal(Priority.MEDIUM, Slices.matching(packageIdentifier));
        }
    }
}
