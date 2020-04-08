package com.tngtech.archunit.core.domain.properties;

import java.io.Serializable;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.javaClassList;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasParameterTypesTest {
    @Test
    public void predicate_on_parameters_by_Class() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(String.class, Serializable.class))
                .accepts(hasParameterTypes)
                .hasDescription("raw parameter types [java.lang.String, java.io.Serializable]");
        assertThat(rawParameterTypes(String.class)).rejects(hasParameterTypes);
        assertThat(rawParameterTypes(Serializable.class)).rejects(hasParameterTypes);
        assertThat(rawParameterTypes(Object.class)).rejects(hasParameterTypes);
    }

    @Test
    public void predicate_on_parameters_by_String() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(String.class.getName(), Serializable.class.getName()))
                .accepts(hasParameterTypes)
                .hasDescription("raw parameter types [java.lang.String, java.io.Serializable]");
        assertThat(rawParameterTypes(String.class.getName())).rejects(hasParameterTypes);
        assertThat(rawParameterTypes(Serializable.class.getName())).rejects(hasParameterTypes);
        assertThat(rawParameterTypes(Object.class.getName())).rejects(hasParameterTypes);
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasParameterTypes hasParameterTypes = newHasParameterTypes(String.class, Serializable.class);

        assertThat(rawParameterTypes(DescribedPredicate.<List<JavaClass>>alwaysTrue()))
                .accepts(hasParameterTypes);
        assertThat(rawParameterTypes(DescribedPredicate.<List<JavaClass>>alwaysFalse().as("some text")))
                .rejects(hasParameterTypes)
                .hasDescription("raw parameter types some text");
    }

    private HasParameterTypes newHasParameterTypes(final Class<?>... parameters) {
        return new HasParameterTypes() {

            @Override
            public JavaClassList getRawParameterTypes() {
                return javaClassList(parameters);
            }
        };
    }
}