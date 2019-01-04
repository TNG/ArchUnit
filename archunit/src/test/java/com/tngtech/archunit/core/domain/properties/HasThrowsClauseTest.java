package com.tngtech.archunit.core.domain.properties;

import java.io.Serializable;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.ThrowsClause;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.throwsClause;
import static org.assertj.core.api.Assertions.assertThat;

public class HasThrowsClauseTest {
    @Test
    public void predicate_on_parameters_by_Class() {
        HasThrowsClause hasThrowsClause = newHasThrowsClause(String.class, Serializable.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class, Serializable.class).apply(hasThrowsClause)).as("predicate matches")
                .isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Serializable.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Object.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class, Serializable.class).getDescription())
                .isEqualTo("throws types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_String() {
        HasThrowsClause hasThrowsClause = newHasThrowsClause(String.class, Serializable.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class.getName(), Serializable.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Serializable.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Object.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(String.class.getName(), Serializable.class.getName()).getDescription())
                .isEqualTo("throws types [java.lang.String, java.io.Serializable]");
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasThrowsClause hasThrowsClause = newHasThrowsClause(String.class, Serializable.class);

        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysTrue()).apply(hasThrowsClause)).isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysFalse()).apply(hasThrowsClause)).isFalse();

        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("throws types some text");
    }

    private HasThrowsClause newHasThrowsClause(final Class<?>... throwsDeclarations) {
        return new HasThrowsClause() {
            @Override
            public ThrowsClause getThrowsClause() {
                return throwsClause(throwsDeclarations);
            }
        };
    }
}
