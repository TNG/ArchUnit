package com.tngtech.archunit.core.domain;

import java.io.IOException;
import java.sql.SQLDataException;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importMethod;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class ThrowsClauseTest {

    @Test
    public void containsType() {
        JavaMethod method = importMethod(SomeClass.class, "method");

        assertAllTrue(contains(method, SQLDataException.class));
        assertAllFalse(contains(method, Exception.class));
    }

    @Test
    public void size() {
        assertThat(importMethod(SomeClass.class, "method").getThrowsClause().size())
                .as("size of throws clause").isEqualTo(2);
    }

    @Test
    public void getTypes() {
        JavaMethod method = importMethod(SomeClass.class, "method");

        assertThatClasses(method.getThrowsClause().getTypes()).matchInAnyOrder(IOException.class, SQLDataException.class);
    }

    private void assertAllTrue(Iterable<? extends AbstractBooleanAssert<?>> asserts) {
        for (AbstractBooleanAssert<?> anAssert : asserts) {
            anAssert.isTrue();
        }
    }

    private void assertAllFalse(Iterable<? extends AbstractBooleanAssert<?>> asserts) {
        for (AbstractBooleanAssert<?> anAssert : asserts) {
            anAssert.isFalse();
        }
    }

    private ImmutableList<? extends AbstractBooleanAssert<?>> contains(JavaMethod method, Class<?> type) {
        return ImmutableList.of(
                assertThat(method.getThrowsClause().containsType(type)).as("throws clause contains " + type.getSimpleName()),
                assertThat(method.getThrowsClause().containsType(type.getName())).as("throws clause contains " + type.getSimpleName()),
                assertThat(method.getThrowsClause().containsType(equivalentTo(type))).as("throws clause contains " + type.getSimpleName()));
    }

    @SuppressWarnings({"RedundantThrows", "unused"})
    private static class SomeClass {
        void method() throws IOException, SQLDataException {
        }
    }
}