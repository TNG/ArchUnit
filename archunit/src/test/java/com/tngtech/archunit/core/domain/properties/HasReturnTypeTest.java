package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RAW_RETURN_TYPE;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RETURN_TYPE;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;

public class HasReturnTypeTest {
    @Test
    public void predicate_on_return_type_by_Class() {
        HasReturnType hasReturnTypeString = newHasReturnType(importClassWithContext(String.class));

        assertThat(rawReturnType(String.class))
                .accepts(hasReturnTypeString)
                .hasDescription("raw return type java.lang.String");
        assertThat(rawReturnType(Object.class)).rejects(hasReturnTypeString);
    }

    @Test
    public void predicate_on_return_type_by_String() {
        HasReturnType hasReturnTypeString = newHasReturnType(importClassWithContext(String.class));

        assertThat(rawReturnType(String.class.getName()))
                .accepts(hasReturnTypeString)
                .hasDescription("raw return type java.lang.String");
        assertThat(rawReturnType(String.class.getSimpleName())).rejects(hasReturnTypeString);
        assertThat(rawReturnType(Object.class.getName())).rejects(hasReturnTypeString);
    }

    @Test
    public void predicate_on_return_type_by_Predicate() {
        HasReturnType hasReturnTypeString = newHasReturnType(importClassWithContext(String.class));

        assertThat(rawReturnType(DescribedPredicate.<JavaClass>alwaysTrue()))
                .accepts(hasReturnTypeString);
        assertThat(rawReturnType(DescribedPredicate.<JavaClass>alwaysFalse().as("some text")))
                .rejects(hasReturnTypeString)
                .hasDescription("raw return type some text");
    }

    @Test
    public void function_get_return_type() {
        abstract class TestClass implements Iterable<String> {
        }

        JavaType expectedType = importClassWithContext(TestClass.class).getSuperclass().get();
        JavaClass rawType = expectedType.toErasure();

        assertThatType(GET_RETURN_TYPE.apply(newHasReturnType(expectedType, rawType)))
                .as("result of GET_RETURN_TYPE").isEqualTo(expectedType);
    }

    @Test
    public void function_get_raw_return_type() {
        abstract class TestClass implements Iterable<String> {
        }

        JavaType genericType = importClassWithContext(TestClass.class).getSuperclass().get();
        JavaClass expectedType = genericType.toErasure();

        assertThatType(GET_RAW_RETURN_TYPE.apply(newHasReturnType(genericType, expectedType)))
                .as("result of GET_RAW_RETURN_TYPE").isEqualTo(expectedType);
    }

    private HasReturnType newHasReturnType(final JavaClass rawReturnType) {
        return newHasReturnType(rawReturnType, rawReturnType);
    }

    private HasReturnType newHasReturnType(final JavaType genericReturnType, final JavaClass rawReturnType) {
        return new HasReturnType() {

            @Override
            public JavaType getReturnType() {
                return genericReturnType;
            }

            @Override
            public JavaClass getRawReturnType() {
                return rawReturnType;
            }
        };
    }
}
