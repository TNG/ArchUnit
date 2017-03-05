package com.tngtech.archunit.core;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaMethodCall.Predicates.target;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.core.properties.HasName.Predicates.name;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMethodCallTest {
    @Test
    public void predicate_target() {
        JavaMethodCall call = simulateCall()
                .from(getClass(), "predicate_target")
                .to(getClass(), "toString");

        DescribedPredicate<JavaMethodCall> targetNameToString = target(name("toString"));
        assertThat(targetNameToString.apply(call)).as("Predicate matches").isTrue();
        assertThat(targetNameToString.getDescription()).as("Description").isEqualTo("target name 'toString'");

        DescribedPredicate<JavaMethodCall> targetNameNotToString = target(name("notToString"));
        assertThat(targetNameNotToString.apply(call)).as("Predicate matches").isFalse();
        assertThat(targetNameNotToString.getDescription()).as("Description").isEqualTo("target name 'notToString'");
    }

    @Override
    public String toString() {
        return super.toString();
    }
}