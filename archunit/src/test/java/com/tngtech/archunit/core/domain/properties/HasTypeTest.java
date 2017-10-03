package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasTypeTest {
    @Test
    public void function_getType() {
        assertThat(HasType.Functions.GET_TYPE.apply(newHasType(String.class))).matches(String.class);
    }

    private HasType newHasType(final Class<String> owner) {
        return new HasType() {
            @Override
            public JavaClass getType() {
                return importClassWithContext(owner);
            }
        };
    }
}