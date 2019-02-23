package com.tngtech.archunit.core.domain.properties;

import java.io.Serializable;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.javaClassList;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.parameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static org.assertj.core.api.Assertions.assertThat;

public class HasParameterTypesTest {
    @Test
    public void predicate_on_parameters_by_Class() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(String.class, Serializable.class).apply(hasParameterTypes)).as("predicate matches").isTrue();
        assertThat(rawParameterTypes(String.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(rawParameterTypes(Serializable.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(rawParameterTypes(Object.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(rawParameterTypes(String.class, Serializable.class).getDescription())
                .isEqualTo("raw parameter types [java.lang.String, java.io.Serializable]");

        assertThat(parameterTypes(String.class, Serializable.class).apply(hasParameterTypes)).as("predicate matches").isTrue();
        assertThat(parameterTypes(String.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(parameterTypes(Serializable.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(parameterTypes(Object.class).apply(hasParameterTypes)).as("predicate matches").isFalse();
        assertThat(parameterTypes(String.class, Serializable.class).getDescription())
                .isEqualTo("parameter types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_String() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(String.class.getName(), Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isTrue();
        assertThat(rawParameterTypes(String.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(rawParameterTypes(Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(rawParameterTypes(Object.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(rawParameterTypes(String.class.getName(), Serializable.class.getName()).getDescription())
                .isEqualTo("raw parameter types [java.lang.String, java.io.Serializable]");

        assertThat(parameterTypes(String.class.getName(), Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isTrue();
        assertThat(parameterTypes(String.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(parameterTypes(Serializable.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(parameterTypes(Object.class.getName()).apply(hasParameterTypes))
                .as("predicate matches").isFalse();
        assertThat(parameterTypes(String.class.getName(), Serializable.class.getName()).getDescription())
                .isEqualTo("parameter types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(DescribedPredicate.<List<JavaClass>>alwaysTrue()).apply(hasParameterTypes)).isTrue();
        assertThat(rawParameterTypes(DescribedPredicate.<List<JavaClass>>alwaysFalse()).apply(hasParameterTypes)).isFalse();

        assertThat(rawParameterTypes(DescribedPredicate.<List<JavaClass>>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("raw parameter types some text");

        assertThat(parameterTypes(DescribedPredicate.<JavaClassList>alwaysTrue()).apply(hasParameterTypes)).isTrue();
        assertThat(parameterTypes(DescribedPredicate.<JavaClassList>alwaysFalse()).apply(hasParameterTypes)).isFalse();

        assertThat(parameterTypes(DescribedPredicate.<JavaClassList>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("parameter types some text");
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