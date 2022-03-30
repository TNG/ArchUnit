package com.tngtech.archunit.tooling.examples;

import java.io.Serializable;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.conditions.Conditions;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static junit.framework.TestCase.assertTrue;

@AnalyzeClasses(packages = "com.tngtech.archunit.tooling.examples")
public class ArchJUnit5Test {

    @ArchTest
    public static final ArchRule shouldReportSuccess = classes().that().implement(Serializable.class)
            .should().implement(Serializable.class);

    @ArchTest
    public static final ArchRule shouldReportFailure = classes().that().implement(Serializable.class)
            .should().notImplement(Serializable.class);

    @ArchTest
    public static final ArchRule shouldReportError = classes().that().implement(Serializable.class)
            .should(new ArchCondition<>("Always throw an exception") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    throw new RuntimeException();
                }
            });

    @ArchTest
    @ArchIgnore
    public static final ArchRule shouldBeSkipped = classes().that().implement(Cloneable.class)
            .should().implement(Cloneable.class);

    @ArchTest
    @Conditions.DisabledIfEnvironmentVariable(named = "SKIP_BY_ENV_VARIABLE", matches = "true")
    public static final ArchRule shouldBeSkippedConditionally = classes().that().implement(Cloneable.class)
            .should().implement(Cloneable.class);

    static class ImplementingSerializable implements Serializable, Cloneable {
        @Override
        public ImplementingSerializable clone() {
            try {
                return (ImplementingSerializable) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

}
