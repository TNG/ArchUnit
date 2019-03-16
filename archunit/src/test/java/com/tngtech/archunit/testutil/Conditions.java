package com.tngtech.archunit.testutil;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.JavaClass.namesOf;

public final class Conditions {
    private Conditions() {
    }

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

    public static Condition<JavaCodeUnit> codeUnitWithSignature(final String name, final Class<?>... parameters) {
        return new Condition<JavaCodeUnit>() {
            @Override
            public boolean matches(JavaCodeUnit value) {
                return name.equals(value.getName()) && namesOf(parameters).equals(value.getRawParameterTypes().getNames());
            }
        }.as("matches signature <" + name + ", " + namesOf(parameters) + ">");
    }
}
