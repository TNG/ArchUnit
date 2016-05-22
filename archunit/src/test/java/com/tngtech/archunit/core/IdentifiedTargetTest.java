package com.tngtech.archunit.core;

import java.lang.reflect.Method;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentifiedTargetTest {
    private static final Predicate<Method> IN_TEST_CLASSES = new Predicate<Method>() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean apply(Method input) {
            return ImmutableSet.of(A.class, SubA.class, B.class, C.class, D.class, E.class).contains(input.getDeclaringClass());
        }
    };

    @Test
    public void finds_simple_target() throws Exception {
        IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(A.class, IN_TEST_CLASSES);

        assertThat(target.wasIdentified()).as("target was identified").isTrue();
        assertThat(target.get()).isEqualTo(A.class.getDeclaredMethod("method"));
    }

    @Test
    public void finds_lowest_in_simple_hierarchy() throws Exception {
        IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(SubA.class, IN_TEST_CLASSES);

        assertThat(target.get()).isEqualTo(SubA.class.getDeclaredMethod("method"));
    }

    @Test
    public void finds_lowest_in_ambiguous_hierarchy_but_with_explicitly_overridden_method() throws Exception {
        IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(C.class, IN_TEST_CLASSES);

        assertThat(target.get()).isEqualTo(C.class.getDeclaredMethod("method"));
    }

    @Test
    public void finds_nothing_in_ambiguous_hierarchy_when_not_explicitly_overridden() throws Exception {
        IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(D.class, IN_TEST_CLASSES);

        assertThat(target.wasIdentified()).as("target was identified").isFalse();
    }

    @Test
    public void finds_lowest_in_complex_hierarchy() throws Exception {
        IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(E.class, IN_TEST_CLASSES);

        assertThat(target.get()).isEqualTo(E.class.getDeclaredMethod("method"));
    }

    private interface A {
        void method();
    }

    private interface SubA extends A {
        @Override
        void method();
    }

    private interface B {
        void method();

        void method(String param);
    }

    private interface C extends A, B {
        @Override
        void method();
    }

    private interface D extends A, B {
    }

    private interface E extends C {
        @Override
        void method();
    }
}