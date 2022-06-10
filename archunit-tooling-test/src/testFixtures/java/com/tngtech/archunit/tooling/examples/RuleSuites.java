package com.tngtech.archunit.tooling.examples;

import java.io.Serializable;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static junit.framework.TestCase.assertTrue;

public abstract class RuleSuites {

    public static class ShouldReportSuccessSuite {

        @ArchTest
        public static final ArchRule rule = classes().that().implement(Serializable.class)
                .should().implement(Serializable.class);
    }

    public static class ShouldReportFailureSuite {

        @ArchTest
        public static final ArchRule rule = classes().that().implement(Serializable.class)
                .should().notImplement(Serializable.class);
    }

    public static class ShouldReportErrorSuite {

        @ArchTest
        public static final ArchRule rule = classes().that().implement(Serializable.class)
            .should(new ArchCondition<>("Always throw an exception") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                throw new RuntimeException();
            }
        });
    }

    public static class ShouldBeSkippedSuite {

        @ArchTest
        @ArchIgnore
        public static final ArchRule rule = classes().that().implement(Cloneable.class)
                .should().implement(Cloneable.class);
    }

    public static class ShouldBeSkippedConditionallySuite {

        @ArchTest
        @DisabledIfEnvironmentVariable(named = "SKIP_BY_ENV_VARIABLE", matches = "true")
        public static void rule(JavaClasses classes) {
            assertTrue(classes.that(assignableTo(Cloneable.class)).contain(ArchJUnit5Test.ImplementingSerializable.class));
        }
    }
}
