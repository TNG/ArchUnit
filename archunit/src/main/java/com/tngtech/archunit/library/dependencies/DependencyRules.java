/*
 * Copyright 2017 TNG Technology Consulting GmbH
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;

class DependencyRules {
    static ArchCondition<Slice> beFreeOfCycles() {
        return new SliceCycleArchCondition();
    }

    static ArchRule slicesShouldNotDependOnEachOtherIn(Slices.Transformer inputTransformer) {
        return all(inputTransformer).should(notDependOnEachOther(inputTransformer));
    }

    private static ArchCondition<Slice> notDependOnEachOther(final Slices.Transformer inputTransformer) {
        return new ArchCondition<Slice>("not depend on each other") {
            @Override
            public void check(Slice slice, ConditionEvents events) {
                Slices dependencySlices = inputTransformer.transform(slice.getDependencies());
                for (Slice dependencySlice : dependencySlices) {
                    events.add(SimpleConditionEvent.violated(slice, describe(slice, dependencySlice)));
                }
            }

            private String describe(Slice slice, Slice dependencySlice) {
                return String.format("%s calls %s:%n%s",
                        slice.getDescription(),
                        dependencySlice.getDescription(),
                        joinDependencies(slice, dependencySlice));
            }

            private String joinDependencies(Slice from, Slice to) {
                List<String> parts = new ArrayList<>();
                for (Dependency dependency : new TreeSet<>(from.getDependencies())) {
                    if (to.contains(dependency.getTargetClass())) {
                        parts.add(dependency.getDescription());
                    }
                }
                return Joiner.on(System.lineSeparator()).join(parts);
            }
        };
    }
}
