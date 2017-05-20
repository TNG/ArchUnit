package com.tngtech.archunit.maventest;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class ArchSubLibrary {
    static final String RULE_ON_LEVEL_TWO_DESCRIPTOR = "rule_on_level_two";
    static final String RULE_METHOD_ON_LEVEL_TWO_DESCRIPTOR = "rule_method_on_level_two";

    @ArchTest
    public static final ArchRule rule_on_level_two =
            classes().should(registerCallAs(ArchSubLibrary.class, RULE_ON_LEVEL_TWO_DESCRIPTOR));

    @ArchTest
    public static void rule_method_on_level_two(JavaClasses classes) {
        CalledRuleRecords.register(ArchSubLibrary.class, RULE_METHOD_ON_LEVEL_TWO_DESCRIPTOR);
    }

    static ArchCondition<JavaClass> registerCallAs(final Class<?> ruleDeclaringClass, final String ruleDescriptor) {
        return new ArchCondition<JavaClass>("<just record call>") {
            boolean firstCall = true;

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (firstCall) {
                    CalledRuleRecords.register(ruleDeclaringClass, ruleDescriptor);
                    firstCall = false;
                }
            }
        };
    }
}
