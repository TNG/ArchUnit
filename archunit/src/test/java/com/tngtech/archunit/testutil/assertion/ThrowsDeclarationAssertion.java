package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThatType;

public class ThrowsDeclarationAssertion extends AbstractObjectAssert<ThrowsDeclarationAssertion, ThrowsDeclaration<?>> {
    public ThrowsDeclarationAssertion(ThrowsDeclaration<?> throwsDeclaration) {
        super(throwsDeclaration, ThrowsDeclarationAssertion.class);
    }

    public void matches(Class<?> clazz) {
        assertThatType(actual.getRawType()).as("Type of " + actual).matches(clazz);
    }
}
