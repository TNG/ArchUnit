package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClassList;
import com.tngtech.archunit.core.domain.ThrowsDeclarations;
import org.junit.Test;

import java.io.Serializable;

import static com.tngtech.archunit.core.domain.TestUtils.throwsDeclarations;
import static org.assertj.core.api.Assertions.assertThat;

public class HasThrowsDeclarationsTest {
    @Test
    public void predicate_on_parameters_by_Class() {
        HasThrowsDeclarations hasThrowsDeclarations = newhasThrowsDeclarations(String.class, Serializable.class);

        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class, Serializable.class).apply(hasThrowsDeclarations)).as("predicate matches").isTrue();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class).apply(hasThrowsDeclarations)).as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(Serializable.class).apply(hasThrowsDeclarations)).as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(Object.class).apply(hasThrowsDeclarations)).as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class, Serializable.class).getDescription())
                .isEqualTo("throws types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_String() {
        HasThrowsDeclarations hasThrowsDeclarations = newhasThrowsDeclarations(String.class, Serializable.class);

        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class.getName(), Serializable.class.getName()).apply(hasThrowsDeclarations))
                .as("predicate matches").isTrue();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class.getName()).apply(hasThrowsDeclarations))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(Serializable.class.getName()).apply(hasThrowsDeclarations))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(Object.class.getName()).apply(hasThrowsDeclarations))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(String.class.getName(), Serializable.class.getName()).getDescription())
                .isEqualTo("throws types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasThrowsDeclarations hasThrowsDeclarations = newhasThrowsDeclarations(String.class, Serializable.class);

        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(DescribedPredicate.<ThrowsDeclarations>alwaysTrue()).apply(hasThrowsDeclarations)).isTrue();
        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(DescribedPredicate.<ThrowsDeclarations>alwaysFalse()).apply(hasThrowsDeclarations)).isFalse();

        assertThat(HasThrowsDeclarations.Predicates.throwsDeclarations(DescribedPredicate.<ThrowsDeclarations>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("throws types some text");
    }

    private HasThrowsDeclarations newhasThrowsDeclarations(final Class<?>... throwsDeclarations) {
        return new HasThrowsDeclarations() {
            @Override
            public ThrowsDeclarations getThrowsDeclarations() {
                return throwsDeclarations(throwsDeclarations);
            }
        };
    }
}
