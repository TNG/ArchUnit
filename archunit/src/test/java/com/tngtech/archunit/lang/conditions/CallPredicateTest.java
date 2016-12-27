package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaCodeUnit;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.lang.conditions.CallPredicate.target;
import static org.assertj.core.api.Assertions.assertThat;

public class CallPredicateTest {
    @Test
    public void combination_of_restrictions() {
        JavaCall<?> call = simulateCall().from(SomeClass.class, "otherMethod").to(SomeClass.class, "someMethod", String.class);

        CallPredicate declaredIn = target().isDeclaredIn(SomeClass.class);
        assertThat(declaredIn.apply(call)).as("predicate matches").isTrue();
        assertThat(declaredIn.hasName("someMethod").apply(call)).as("predicate matches").isTrue();
        assertThat(declaredIn.hasName("wrong").apply(call)).as("predicate matches").isFalse();
        assertThat(declaredIn.hasParameters(String.class).apply(call)).as("predicate matches").isTrue();
        assertThat(declaredIn.hasParameters().apply(call)).as("predicate matches").isFalse();
        assertThat(declaredIn.hasName("someMethod").is(DescribedPredicate.<JavaCodeUnit>alwaysTrue()).apply(call))
                .as("predicate matches").isTrue();
        assertThat(declaredIn.hasName("wrong").is(DescribedPredicate.<JavaCodeUnit>alwaysTrue()).apply(call))
                .as("predicate matches").isFalse();
        assertThat(declaredIn.hasName("someMethod").is(DescribedPredicate.<JavaCodeUnit>alwaysFalse()).apply(call))
                .as("predicate matches").isFalse();

        assertThat(declaredIn.hasName("someMethod").hasParameters(String.class).apply(call))
                .as("predicate matches").isTrue();
    }

    @Test
    public void descriptions() {
        CallPredicate predicate = target().is(SomeClass.class, "someMethod", String.class);
        assertThat(predicate.getDescription())
                .isEqualTo(String.format("target is %s.someMethod(String.class)", SomeClass.class.getName()));

        predicate = target().isDeclaredIn(SomeClass.class).hasName("someMethod").hasParameters(String.class);
        assertThat(predicate.getDescription())
                .isEqualTo("target is declared in SomeClass and has name 'someMethod' and has parameters [String.class]");
    }

    private static class SomeClass {
        void someMethod(String arg) {
        }

        void otherMethod() {
        }
    }
}