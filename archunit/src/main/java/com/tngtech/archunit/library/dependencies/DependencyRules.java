package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.ArchRule.rule;

public class DependencyRules {
    public static ArchCondition<Slice> noCycles() {
        return new SlicesArchCondition();
    }

    public static ArchRule<Slice> noCyclesIn(Slices.Transformer inputTransformer) {
        return rule(inputTransformer).should("be free of cycles").assertedBy(noCycles());
    }
}
