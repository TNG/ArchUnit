package com.tngtech.archunit.testutil.assertion;

import java.util.Set;

import com.tngtech.archunit.core.Convertible;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;

public class ConversionAssertion extends AbstractObjectAssert<ConversionAssertion, Convertible> {
    public ConversionAssertion(Convertible actual) {
        super(actual, ConversionAssertion.class);
    }

    public ConversionAssertion isPossibleTo(Class<?> type) {
        assertThat(actual.convertTo(type))
                .as(String.format("conversion of %s to compatible type %s", actual.getClass().getName(), type.getName()))
                .isNotEmpty();
        return this;
    }

    public ConversionAssertion isNotPossibleTo(Class<?> type) {
        assertThat(actual.convertTo(type))
                .as(String.format("conversion of %s to incompatible type %s", actual.getClass().getName(), type.getName()))
                .isEmpty();
        return this;
    }

    public <T> ConversionAssertion isPossibleToSingleElement(Class<T> type, ResultAssertion<? super T> resultAssertion) {
        Set<T> converted = actual.convertTo(type);
        assertThat(converted)
                .as(String.format("result of converting %s to %s", actual.getClass().getName(), type.getName()))
                .hasSize(1);
        resultAssertion.assertResult(getOnlyElement(converted));
        return this;
    }

    public ConversionAssertion satisfiesStandardConventions() {
        class ImpossibleToConvertTo {
        }
        return isNotPossibleTo(ImpossibleToConvertTo.class)
                .isPossibleToSingleElement(Object.class, it -> assertThat(it).isInstanceOf(actual.getClass()));
    }

    public interface ResultAssertion<T> {
        void assertResult(T result);
    }
}