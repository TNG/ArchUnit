package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.properties.HasReturnType.Predicates.withReturnType;
import static org.assertj.core.api.Assertions.assertThat;

public class HasReturnTypeTest {
    @Test
    public void predicate_on_return_type_by_Class() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(withReturnType(String.class).apply(hasReturnTypeString)).as("predicate matches").isTrue();
        assertThat(withReturnType(Object.class).apply(hasReturnTypeString)).as("predicate matches").isFalse();
        assertThat(withReturnType(String.class).getDescription()).isEqualTo("with return type 'java.lang.String'");
    }

    @Test
    public void predicate_on_return_type_by_String() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(withReturnType(String.class.getName()).apply(hasReturnTypeString)).as("predicate matches").isTrue();
        assertThat(withReturnType(String.class.getSimpleName()).apply(hasReturnTypeString)).as("predicate matches").isFalse();
        assertThat(withReturnType(Object.class.getName()).apply(hasReturnTypeString)).as("predicate matches").isFalse();

        assertThat(withReturnType(String.class.getName()).getDescription()).isEqualTo("with return type 'java.lang.String'");
    }

    @Test
    public void predicate_on_return_type_by_Predicate() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(withReturnType(DescribedPredicate.<JavaClass>alwaysTrue()).apply(hasReturnTypeString)).isTrue();
        assertThat(withReturnType(DescribedPredicate.<JavaClass>alwaysFalse()).apply(hasReturnTypeString)).isFalse();

        assertThat(withReturnType(DescribedPredicate.<JavaClass>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("with return type 'some text'");
    }

    @Test
    public void function_get_return_type() {
        JavaClass expectedType = javaClassViaReflection(String.class);
        assertThat(HasReturnType.Functions.GET_RETURN_TYPE.apply(newHasReturnType(expectedType)))
                .as("result of GET_RETURN_TYPE").isEqualTo(expectedType);
    }

    private HasReturnType newHasReturnType(final JavaClass javaClass) {
        return new HasReturnType() {
            @Override
            public JavaClass getReturnType() {
                return javaClass;
            }
        };
    }
}