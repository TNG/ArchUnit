package com.tngtech.archunit.core.properties;

import java.io.Serializable;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClassList;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClassList;
import static com.tngtech.archunit.core.properties.HasParameterTypes.Predicates.withParameterTypes;
import static org.assertj.core.api.Assertions.assertThat;

public class HasParameterTypesTest {
    @Test
    public void predicate_on_parameters_by_Class() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(withParameterTypes(String.class, Serializable.class).apply(hasParameterTypes)).as("predicate matches").isTrue();
        assertThat(withParameterTypes(String.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(withParameterTypes(Serializable.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(withParameterTypes(Object.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(withParameterTypes(String.class, Serializable.class).getDescription())
                .isEqualTo("with parameter types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_String() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(withParameterTypes(String.class.getName(), Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isTrue();
        assertThat(withParameterTypes(String.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(withParameterTypes(Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(withParameterTypes(Object.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(withParameterTypes(String.class.getName(), Serializable.class.getName()).getDescription())
                .isEqualTo("with parameter types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(withParameterTypes(DescribedPredicate.<JavaClassList>alwaysTrue()).apply(hasParameterTypes)).isTrue();
        assertThat(withParameterTypes(DescribedPredicate.<JavaClassList>alwaysFalse()).apply(hasParameterTypes)).isFalse();

        assertThat(withParameterTypes(DescribedPredicate.<JavaClassList>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("with parameter types some text");
    }

    private HasParameterTypes newHasParameterTypes(final Class<?>... parameters) {
        return new HasParameterTypes() {
            @Override
            public JavaClassList getParameters() {
                return javaClassList(parameters);
            }
        };
    }
}