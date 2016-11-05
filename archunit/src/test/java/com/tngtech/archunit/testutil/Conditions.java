package com.tngtech.archunit.testutil;

import java.util.List;

import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.TypeDetails;
import org.assertj.core.api.Condition;

public final class Conditions {
    private Conditions() {}

    public static <T> Condition<Iterable<? extends T>> containing(final Condition<T> condition) {
        return new Condition<Iterable<? extends T>>() {
            @Override
            public boolean matches(Iterable<? extends T> value) {
                boolean contains = false;
                for (T t : value) {
                    contains = contains || condition.matches(t);
                }
                return contains;
            }
        }.as("containing an element that " + condition.description());
    }

    public static Condition<JavaCodeUnit<?, ?>> codeUnitWithSignature(final String name, final Class<?>... parameters) {
        final List<TypeDetails> paramList = TypeDetails.allOf(parameters);
        return new Condition<JavaCodeUnit<?, ?>>() {
            @Override
            public boolean matches(JavaCodeUnit<?, ?> value) {
                return name.equals(value.getName()) && paramList.equals(value.getParameters());
            }
        }.as("matches signature <" + name + ", " + paramList + ">");
    }
}
