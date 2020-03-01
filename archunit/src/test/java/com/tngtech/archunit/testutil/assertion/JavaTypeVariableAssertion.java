package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaTypeVariable;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class JavaTypeVariableAssertion extends AbstractObjectAssert<JavaTypeVariableAssertion, JavaTypeVariable> {
    public JavaTypeVariableAssertion(JavaTypeVariable actual) {
        super(actual, JavaTypeVariableAssertion.class);
    }

    public void hasBoundsMatching(Class<?>... bounds) {
        assertThatTypes(actual.getBounds()).as("Type variable bounds").matchExactly(bounds);
    }
}
