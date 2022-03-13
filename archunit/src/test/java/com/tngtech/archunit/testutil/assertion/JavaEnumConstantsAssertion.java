package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaEnumConstant;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaEnumConstantsAssertion extends AbstractObjectAssert<JavaEnumConstantsAssertion, JavaEnumConstant[]> {
    public JavaEnumConstantsAssertion(JavaEnumConstant[] enumConstants) {
        super(enumConstants, JavaEnumConstantsAssertion.class);
    }

    public void matches(Enum<?>... enumConstants) {
        assertThat((Object[]) actual).as("Enum constants").hasSize(enumConstants.length);
        for (int i = 0; i < actual.length; i++) {
            assertThat(actual[i]).as("Element %d", i).isEquivalentTo(enumConstants[i]);
        }
    }
}
