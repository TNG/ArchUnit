package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class RuleThatFails {
    public static ArchRule on(Class<?> input) {
        return classes().should(new ArchCondition<JavaClass>("not be " + input.getName()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.isEquivalentTo(input)) {
                    events.add(SimpleConditionEvent.violated(item, "Got class " + item.getName()));
                }
            }
        });
    }
}
