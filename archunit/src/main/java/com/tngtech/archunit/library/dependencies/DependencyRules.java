package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.Dependency;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

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
                    events.add(ConditionEvent.violated("%s calls %s:%n%s",
                            slice.getDescription(), dependencySlice.getDescription(), joinDependencies(slice, dependencySlice)));
                }
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
