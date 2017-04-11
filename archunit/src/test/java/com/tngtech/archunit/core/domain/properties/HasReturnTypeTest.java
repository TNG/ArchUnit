package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.returnType;
import static org.assertj.core.api.Assertions.assertThat;

public class HasReturnTypeTest {
    @Test
    public void predicate_on_return_type_by_Class() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(returnType(String.class).apply(hasReturnTypeString)).as("predicate matches").isTrue();
        assertThat(returnType(Object.class).apply(hasReturnTypeString)).as("predicate matches").isFalse();
        assertThat(returnType(String.class).getDescription()).isEqualTo("return type 'java.lang.String'");
    }

    @Test
    public void predicate_on_return_type_by_String() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(returnType(String.class.getName()).apply(hasReturnTypeString)).as("predicate matches").isTrue();
        assertThat(returnType(String.class.getSimpleName()).apply(hasReturnTypeString)).as("predicate matches").isFalse();
        assertThat(returnType(Object.class.getName()).apply(hasReturnTypeString)).as("predicate matches").isFalse();

        assertThat(returnType(String.class.getName()).getDescription()).isEqualTo("return type 'java.lang.String'");
    }

    @Test
    public void predicate_on_return_type_by_Predicate() {
        HasReturnType hasReturnTypeString = newHasReturnType(javaClassViaReflection(String.class));

        assertThat(returnType(DescribedPredicate.<JavaClass>alwaysTrue()).apply(hasReturnTypeString)).isTrue();
        assertThat(returnType(DescribedPredicate.<JavaClass>alwaysFalse()).apply(hasReturnTypeString)).isFalse();

        assertThat(returnType(DescribedPredicate.<JavaClass>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("return type 'some text'");
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